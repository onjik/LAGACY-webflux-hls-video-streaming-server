package com.oj.videostreamingserver.domain.vod.handler;

import com.oj.videostreamingserver.domain.vod.component.EncodingChannel;
import com.oj.videostreamingserver.domain.vod.domain.VideoEntry;
import com.oj.videostreamingserver.domain.vod.domain.VideoMediaEntry;
import com.oj.videostreamingserver.domain.vod.dto.domain.EncodingEvent;
import com.oj.videostreamingserver.domain.vod.dto.domain.EncodingRequestForm;
import com.oj.videostreamingserver.domain.vod.dto.request.ThumbnailPatchRequest;
import com.oj.videostreamingserver.domain.vod.dto.request.VideoPostRequest;
import com.oj.videostreamingserver.domain.vod.dto.response.SingleEncodingStatusResponse;
import com.oj.videostreamingserver.domain.vod.dto.response.VideoPostResponse;
import com.oj.videostreamingserver.domain.vod.service.EncodingService;
import com.oj.videostreamingserver.domain.vod.service.FileService;
import com.oj.videostreamingserver.global.error.exception.InvalidInputValueException;
import com.oj.videostreamingserver.global.util.ConverterUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.r2dbc.core.R2dbcEntityTemplate;
import org.springframework.data.relational.core.query.Query;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.codec.multipart.FilePart;
import org.springframework.stereotype.Component;
import org.springframework.transaction.reactive.TransactionalOperator;
import org.springframework.web.reactive.function.server.ServerRequest;
import org.springframework.web.reactive.function.server.ServerResponse;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.nio.file.Path;
import java.util.*;

import static com.oj.videostreamingserver.domain.vod.util.PathManager.*;
import static org.springframework.data.relational.core.query.Criteria.where;

/**
 * POST /media : 원본 비디오 포스팅 용 API 핸들러
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class EncodingHandler {




    //spring beans
    private final EncodingService encodingService;
    private final FileService fileService;

    private final EncodingChannel encodingChannel;
    private final TransactionalOperator transactionalOperator;

    private final R2dbcEntityTemplate template;


    //처음 비디오를 등록할 때 사용하는 핸들러
    public Mono<ServerResponse> insertVideoDomain(ServerRequest request){
        return VideoPostRequest.from(request)//InvalidInputValueException
                //비디오 아이디가 유효한지 확인
                .flatMap(requestBody -> {
                    return template.exists(Query.query(where("video_id").is(ConverterUtil.convertToByte(requestBody.getVideoId()))), VideoEntry.class)
                            .filter(exists -> exists)
                            .switchIfEmpty(Mono.defer(() -> Mono.error(new InvalidInputValueException("videoId",requestBody.getVideoId().toString(),"invalid videoId"))))
                            .then(Mono.just(requestBody));
                })
                //이미 존재하는 파일인지와, 인코딩 큐에 들어가 있는지를 동시에 확인
                .flatMap(requestBody -> {
                    return template.exists(Query.query(where("video_id").is(ConverterUtil.convertToByte(requestBody.getVideoId()))), VideoMediaEntry.class)
                            .filter(exists -> !exists)
                            .switchIfEmpty(Mono.defer(() -> Mono.error(new InvalidInputValueException("videoId",requestBody.getVideoId().toString(),"already registered video"))))
                            .then(Mono.just(requestBody));
                })
                .flatMap(requestBody -> {
                    UUID videoId = requestBody.getVideoId();
                    Mono<Boolean> isVideoEncodingRegistered = Mono.defer(() -> Mono.just(encodingChannel.contains(videoId, EncodingChannel.Type.VIDEO)));
                    Mono<Boolean> isThumbnailEncodingRegistered = Mono.defer(() -> Mono.defer(() -> Mono.just(encodingChannel.contains(videoId, EncodingChannel.Type.THUMBNAIL))));

                    return Mono.zip(isVideoEncodingRegistered, isThumbnailEncodingRegistered)
                            .filter(zip -> !zip.getT1())
                            .filter(zip -> !zip.getT2())
                            .switchIfEmpty(Mono.defer(() -> Mono.error(new InvalidInputValueException("videoId",requestBody.getVideoId().toString(),"already registered video"))))
                            .then(Mono.just(requestBody));
                })
                // main logic
                // 주어진 파일 저장
                .publishOn(Schedulers.boundedElastic()) //아래의 작업은 블로킹이 심하므로, 별도의 쓰레드로 분리
                .flatMap(requestBody -> {
                    FilePart videoFile = requestBody.getVideoFile();
                    FilePart thumbnail = requestBody.getThumbnail();

                    UUID videoId = requestBody.getVideoId();

                    Path ogVideoPath = VodPath.ogVideoOf(videoId, videoFile.filename());

                    //오리지널 파일을 저장한다.
                    return fileService.saveFilePart(videoFile, ogVideoPath)
                            //썸네일을 저장한다.
                            .then(Mono.justOrEmpty(thumbnail))
                            .flatMap(thumbnailFile -> fileService.saveFilePart(thumbnailFile, VodPath.ogThumbnailOf(videoId, thumbnail.filename()))
                                    .then(Mono.just(VodPath.ogThumbnailOf(videoId, thumbnail.filename())))
                            )
                            .switchIfEmpty(Mono.defer(() -> Mono.just(ogVideoPath)))
                            //썸네일 인코딩
                            .flatMap(encodingTargetPath -> encodingService.encodeThumbnail(videoId,encodingTargetPath.toFile()))
                            //인코딩 요청서 작성
                            .then(Mono.just(EncodingRequestForm.builder()
                                            .videoId(videoId)
                                            .ogVideoPath(ogVideoPath)
                                            .resolutionCandidates(List.of(1080, 720, 480, 360))
                                            .build()));
                })
                //비디오 인코딩 요청
                .flatMap(encodingRequest -> {
                    UUID videoId = encodingRequest.getVideoId();
                    Path ogVideoPath = encodingRequest.getOgVideoPath();
                    List<Integer> resolutionCandidates = encodingRequest.getResolutionCandidates();
                    //오리지널 파일을 저장한다.
                    return encodingService.encodeVideo(videoId, ogVideoPath, resolutionCandidates)
                            .flatMap(probeResult -> Mono.just(new VideoPostResponse(videoId.toString(), probeResult.getStreams().get(0).duration)));
                })
                //정상적인 응답 작성
                .flatMap(videoPostResponse -> ServerResponse.status(HttpStatus.OK).bodyValue(videoPostResponse));
    }


    public Mono<ServerResponse> broadCastEncodingStatus(ServerRequest request) {
        String videoId = request.pathVariable("videoId");
        return Mono.just(encodingChannel.getEncodingEvent(UUID.fromString(videoId), EncodingChannel.Type.VIDEO))
                .filter(Optional::isPresent)
                .switchIfEmpty(Mono.defer(() -> Mono.error(new InvalidInputValueException("videoId", videoId, "not registered videoId"))))
                .map(Optional::get)
                .flatMap(encodingEvent -> {
                    if (encodingEvent.getStatus().equals(EncodingEvent.Status.RUNNING)) {
                        Flux<String> eventFlux = encodingEvent.getFlux();
                        return ServerResponse.ok().contentType(MediaType.TEXT_EVENT_STREAM).body(eventFlux, String.class);
                    } else {
                        return ServerResponse.ok().bodyValue(encodingEvent.getStatus());
                    }
                });
    }

    public Mono<ServerResponse> getEncodingStatus(ServerRequest request) {
        UUID videoId = UUID.fromString(request.pathVariable("videoId"));
        return Mono.just(encodingChannel.selectByVideoId(videoId))
                .filter(map -> !map.isEmpty())
                .flatMap(map -> {
                    Mono<EncodingEvent.Status> entireStatus = Mono.justOrEmpty(map.values().stream()
                            .map(Enum::ordinal)
                            .max(Integer::compareTo)
                            .map(maxOrdinal -> EncodingEvent.Status.values()[maxOrdinal]));
                    return entireStatus
                            .flatMap(status -> Mono.just(new SingleEncodingStatusResponse(map,status)));
                })
                .switchIfEmpty(Mono.defer(() -> {
                    return template.exists(Query.query(where("video_id").is(ConverterUtil.convertToByte(videoId))), VideoMediaEntry.class)
                            .filter(exists -> exists)
                            .switchIfEmpty(Mono.defer(() -> Mono.error(new InvalidInputValueException("videoId", videoId.toString(), "not registered videoId"))))
                            .then(Mono.just(new SingleEncodingStatusResponse(EncodingEvent.Status.COMPLETE)));
                }))
                .flatMap(body -> ServerResponse.ok().bodyValue(body));
    }


    public Mono<ServerResponse> deleteVideoDomain(ServerRequest request) {
        return Mono.just(request.pathVariable("videoId"))
                .map(UUID::fromString)
                //DB 에 존재하는지 체크
                .flatMap(videoId -> template.exists(Query.query(where("video_id").is(ConverterUtil.convertToByte(videoId))), VideoMediaEntry.class)
                        .filter(exists -> exists)
                        .switchIfEmpty(Mono.defer(() -> Mono.error(new InvalidInputValueException("videoId", request.pathVariable("videoId"), "not registered videoId"))))
                        .then(Mono.just(videoId)))
                .publishOn(Schedulers.boundedElastic())
                //트랜잭션
                .as(transactionalOperator::transactional)
                //DB 에서 삭제
                .flatMap(videoId -> template.delete(VideoMediaEntry.class)
                        .from("video_media")
                        .matching(Query.query(where("video_id").is(ConverterUtil.convertToByte(videoId))))
                        .all()
                        .onErrorResume(ClassCastException.class,e -> Mono.empty()) //라이브러리 자체 이슈 때문에 임시 조치
                        .then(Mono.just(VodPath.rootOf(videoId))))
                //존재하면 파일 시스템에서 삭제
                .filter(rootPath -> rootPath.toFile().exists())
                .flatMap(fileService::deleteFile)
                .then(ServerResponse.ok().build());
    }

    public Mono<ServerResponse> updateThumbnail(ServerRequest request){
        return ThumbnailPatchRequest.from(request)
                //일단 도메인과 썸네일이 있는지 확인
                .flatMap(patchRequest -> template.exists(Query.query(where("video_id").is(ConverterUtil.convertToByte(patchRequest.getVideoId()))), VideoMediaEntry.class)
                        .filter(exists -> exists)
                        .switchIfEmpty(Mono.defer(() -> Mono.error(new InvalidInputValueException("videoId",request.pathVariable("videoId"),"not registered videoId"))))
                        .then(Mono.just(patchRequest)))
                //일단 임시로 저장
                .flatMap(patchRequest -> {
                    Path tempSavePath = VodPath.ogThumbnailOf(patchRequest.getVideoId(), patchRequest.getThumbnail().filename());
                    return fileService.saveFilePart(patchRequest.getThumbnail(),tempSavePath)
                                    .then(Mono.zip(Mono.just(patchRequest), Mono.just(tempSavePath)));
                })
                //기존의 썸네일 위치에 인코딩을 시작(덮어 쓰기)
                .flatMap(zip -> {
                    ThumbnailPatchRequest patchRequest = zip.getT1();
                    Path tempSavePath = zip.getT2();
                    return encodingService.encodeThumbnail(patchRequest.getVideoId(), tempSavePath.toFile())
                            .then(Mono.just(tempSavePath));
                })
                .then(ServerResponse.ok().build());
    }
}
