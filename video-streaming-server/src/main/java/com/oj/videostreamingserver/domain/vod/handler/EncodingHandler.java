package com.oj.videostreamingserver.domain.vod.handler;

import com.oj.videostreamingserver.domain.vod.component.EncodingChannel;
import com.oj.videostreamingserver.domain.vod.component.PathManager;
import com.oj.videostreamingserver.domain.vod.dto.domain.EncodingEvent;
import com.oj.videostreamingserver.domain.vod.dto.domain.EncodingRequestForm;
import com.oj.videostreamingserver.domain.vod.dto.request.ThumbnailPatchRequest;
import com.oj.videostreamingserver.domain.vod.dto.request.VideoPostRequest;
import com.oj.videostreamingserver.domain.vod.dto.response.VideoPostResponse;
import com.oj.videostreamingserver.domain.vod.service.EncodingService;
import com.oj.videostreamingserver.domain.vod.service.FileService;
import com.oj.videostreamingserver.global.error.exception.InvalidInputValueException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.codec.multipart.FilePart;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.server.ServerRequest;
import org.springframework.web.reactive.function.server.ServerResponse;
import reactor.core.publisher.Mono;
import reactor.core.publisher.Sinks;

import java.nio.file.Path;
import java.util.*;

import static com.oj.videostreamingserver.domain.vod.component.PathManager.*;

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

//    private final TransactionalOperator transactionalOperator;
//
//    private final R2dbcEntityTemplate template;



    /**
     * 비디오, 썸네일 첫 포스팅 핸들러 <br>
     * @param request 서버 요청
     * @return 서버 응답
     * @apiNote 비디오 포스팅 API
     * @implNote 인코딩 큐에 등록만 하고, 등록이 성공하면, 200 응답을 보낸다. <br>
     * 그러므로 200 응답을 받았다고 해서 인코딩이 성공했다는 말은 아니고, 인코딩 큐에 등록이 성공했다는 뜻이다.
     */
    public Mono<ServerResponse> insertVideoDomain(ServerRequest request){
        return VideoPostRequest.from(request)//InvalidInputValueException
                //이미 등록된 비디오인지 확인
                .flatMap(requestBody -> {
                    String videoId = requestBody.getVideoId().toString();
                    if (encodingChannel.isRegistered(videoId)) {
                        return Mono.error(new InvalidInputValueException("encodingRequest",videoId,"already registered encoding request"));
                    }
                    if (PathManager.VodPath.rootOf(requestBody.getVideoId()).toFile().exists()){
                        return Mono.error(new InvalidInputValueException("videoId",videoId,"already registered video"));
                    }
                    return Mono.just(requestBody);
                })
                //오리지널 파일 저장 로직 - 여기까지는 트랜잭션 대상임
                .flatMap(requestBody -> {
                    FilePart videoFile = requestBody.getVideoFile();
                    FilePart thumbnail = requestBody.getThumbnail();

                    UUID videoId = requestBody.getVideoId();

                    Path ogVideoPath = PathManager.VodPath.ogVideoOf(videoId, videoFile.filename());
                    Path thumbnailPath = VodPath.thumbnailOf(videoId);

                    //오리지널 파일을 저장한다.
                    return fileService.saveFilePart(videoFile, ogVideoPath)
                            //썸네일을 저장한다.
                            .then(Mono.justOrEmpty(thumbnail))
                            //썸네일이 주어졌을 경우 - 주어진 썸네일로 인코딩
                            .flatMap(thumbnailFile -> fileService.saveFilePart(thumbnailFile, thumbnailPath)
                                    .then(encodingService.encodeThumbnail(videoId, thumbnailPath.toFile()))
                            )
                            //썸네일이 주어지지 않았을 경우 - 썸네일을 추출한다.
                            .switchIfEmpty(Mono.defer(() -> encodingService.encodeThumbnail(videoId, ogVideoPath.toFile())))
                            //인코딩 요청서 작성
                            .then(Mono.just(EncodingRequestForm.builder()
                                            .videoId(videoId)
                                            .ogVideoPath(ogVideoPath)
                                            .resolutionCandidates(List.of(1080, 720, 480, 360))
                                            .build()));
                })
                //비디오 인코딩 - 여기서 부터는 별도의 쓰레드에 제어권이 넘어가므로, 에러가 나도 서버측에서 알아서 처리해야함
                .flatMap(encodingRequest -> {
                    Path ogVideoPath = encodingRequest.getOgVideoPath();
                    List<Integer> resolutionCandidate = List.of(1080, 720, 480, 360);
                    //오리지널 파일을 저장한다.
                    return encodingService.encodeVideo(encodingRequest.getVideoId(), ogVideoPath, resolutionCandidate)
                            .then(Mono.just(encodingRequest.getVideoId()));
                })
                //정상적인 응답 작성
                .flatMap(videoId -> ServerResponse.status(HttpStatus.OK).bodyValue(new VideoPostResponse(videoId.toString())));
    }


    public Mono<ServerResponse> broadCastEncodingStatus(ServerRequest request){
        String videoId = request.pathVariable("videoId");
        return Mono.just(encodingChannel.keyResolver(videoId, EncodingChannel.Type.VIDEO))
                .flatMap(key -> {
                    if (encodingChannel.getStatus(key).equals(EncodingEvent.Status.RUNNING)){
                        Sinks.Many<String> sink = encodingChannel.getSink(key).orElseThrow(() -> new InvalidInputValueException("videoId",videoId,"not registered videoId"));
                        return ServerResponse.ok().contentType(MediaType.TEXT_EVENT_STREAM).body(sink.asFlux(), String.class);
                    } else {
                        return ServerResponse.ok().bodyValue(encodingChannel.getStatus(key));
                    }
                })
                .onErrorResume(IllegalArgumentException.class, e -> Mono.error(new InvalidInputValueException("videoId",videoId,"not registered videoId")));
    }



    public Mono<ServerResponse> deleteVideoDomain(ServerRequest request) {
        return Mono.just(request.pathVariable("videoId"))
                .map(videoId -> PathManager.VodPath.rootOf(UUID.fromString(videoId)))
                .filter(rootPath -> rootPath.toFile().exists())
                //존재 안하면 404
                .flatMap(rootPath -> {
                    if (rootPath.toFile().exists()){
                        return Mono.just(rootPath);
                    } else {
                        return Mono.error(new InvalidInputValueException("videoId",rootPath.getFileName().toString(),"not registered videoId"));
                    }
                })
                //존재하면 삭제
                .flatMap(fileService::deleteFile)
                .then(ServerResponse.ok().build());
    }

    public Mono<ServerResponse> updateThumbnail(ServerRequest request){
        return ThumbnailPatchRequest.from(request)
                //일단 도메인과 썸네일이 있는지 확인
                .filter(patchRequest -> PathManager.VodPath.thumbnailOf(patchRequest.getVideoId()).toFile().exists())
                .switchIfEmpty(Mono.defer(() -> Mono.error(new InvalidInputValueException("videoId",request.pathVariable("videoId"),"not registered videoId"))))
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
                //기존 파일 삭제
                .flatMap(fileService::deleteFile)
                .then(ServerResponse.ok().build());
    }
}
