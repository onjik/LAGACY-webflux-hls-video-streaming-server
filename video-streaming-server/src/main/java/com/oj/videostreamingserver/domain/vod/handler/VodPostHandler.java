package com.oj.videostreamingserver.domain.vod.handler;

import com.oj.videostreamingserver.domain.vod.dto.EncodingRequestForm;
import com.oj.videostreamingserver.domain.vod.dto.VodPostRequestBody;
import com.oj.videostreamingserver.domain.vod.component.EncodingChannel;
import com.oj.videostreamingserver.domain.vod.service.EncodingService;
import com.oj.videostreamingserver.global.error.ErrorResponse;
import com.oj.videostreamingserver.global.error.exception.InvalidInputValueException;
import com.oj.videostreamingserver.global.error.exception.LocalSystemException;
import com.oj.videostreamingserver.global.util.FFmpegProcessUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.bramp.ffmpeg.FFmpeg;
import net.bramp.ffmpeg.FFprobe;
import org.springframework.data.r2dbc.core.R2dbcEntityTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.http.codec.multipart.FilePart;
import org.springframework.stereotype.Component;
import org.springframework.transaction.reactive.TransactionalOperator;
import org.springframework.web.reactive.function.server.ServerRequest;
import org.springframework.web.reactive.function.server.ServerResponse;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

import static com.oj.videostreamingserver.domain.vod.component.PathManager.*;

/**
 * POST /media : 원본 비디오 포스팅 용 API 핸들러
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class VodPostHandler {




    //spring beans
    private final FFmpeg fFmpeg;
    private final FFprobe fFprobe;
    private final FFmpegProcessUtil fFmpegProcessUtil;

    private final EncodingService encodingService;
    private final EncodingChannel encodingChannel;

    private final TransactionalOperator transactionalOperator;

    private final R2dbcEntityTemplate template;



    /**
     * 비디오, 썸네일 포스팅 핸들러
     * @param request 서버 요청
     * @return 서버 응답
     */
    public Mono<ServerResponse> videoInitPost(ServerRequest request){
        Long channelId = 1L;
        return VodPostRequestBody.monoFromServerRequest(request)
                //오리지널 비디오 파일 저장
                .flatMap(requestBody -> {
                    FilePart videoFile = requestBody.getVideoFile();
                    UUID videoId = requestBody.getVideoId();
                    Path ogVideoPath = VodPath.ogVideoOf(videoId, videoFile.filename());
                    //폴더 생성
                    return Mono.fromCallable(() -> Files.createDirectories(ogVideoPath.getParent()))
                            .subscribeOn(Schedulers.boundedElastic())//블로킹을 방지하기 위해 별도의 쓰레드에서 분리 실행
                            //그후 비디오 파일 저장
                            .then(videoFile.transferTo(VodPath.ogVideoOf(videoId, videoFile.filename())))
                            .onErrorResume(IllegalStateException.class, e -> Mono.error(new InvalidInputValueException("videoField","","파일이 아닙니다.")))
                            //두개를 묶어서 다음으로 전달
                            .then(Mono.zip(Mono.just(requestBody), Mono.just(ogVideoPath)));
                }) // 여기서 발생 가능한 예외 : InvalidInputValueException , 그 외 파일 저장 관련 예외
                //임시 썸네일 파일 저장
                .flatMap(zip -> {
                    VodPostRequestBody requestBody = zip.getT1();
                    Path ogVideoPath = zip.getT2();
                    return Mono.justOrEmpty(requestBody.getThumbnail())
                            //썸네일이 주어졌을 경우 - 썸네일을 임시 썸네일로 저장
                            .flatMap(thumbnail -> {
                                Path tempThumbnail = VodPath.tempThumbnailOf(requestBody.getVideoId(), thumbnail.filename());
                                return Mono.fromCallable(() -> Files.createDirectories(tempThumbnail.getParent()))
                                        .subscribeOn(Schedulers.boundedElastic())
                                        .then(thumbnail.transferTo(tempThumbnail))
                                        .onErrorResume(IllegalStateException.class, e -> Mono.error(new InvalidInputValueException("videoField","","파일이 아닙니다.")))
                                        .then(Mono.just(tempThumbnail));
                            })
                            //썸네일이 주어지지 않았을 경우
                            .switchIfEmpty(Mono.fromCallable(() -> {
                                //첫번째 프레임 추출해서 임시 썸네일로 저장
                                Path tempThumbnail = VodPath.tempThumbnailOf(requestBody.getVideoId());
                                encodingService.extractFirstFrame(ogVideoPath, tempThumbnail);
                                return tempThumbnail;
                            }).subscribeOn(Schedulers.boundedElastic())) //별도의 쓰레드에서 실행(블로킹 방지)
                            //원본 비디오와 임시 썸네일을 다음으로 전달
                            .flatMap(tempThumbnail -> Mono.zip(Mono.just(requestBody.getVideoId()), Mono.just(ogVideoPath), Mono.just(tempThumbnail)));
                }) //여기서 발생 가능한 예외 : InvalidInputValueException , KernelProcessException , 그외 파일 저장 관련 예외
                //인코딩 요청서 작성
                .flatMap(tuple -> {
                    //전달 받은 값들
                    UUID videoId = tuple.getT1();
                    Path ogVideoPath = tuple.getT2();
                    Path tempThumbnail = tuple.getT3();
                    return Mono.just(EncodingRequestForm.builder()
                            .videoId(videoId)
                            .ogVideoPath(ogVideoPath)
                            .tempThumbnailPath(tempThumbnail)
                            .resolutionCandidates(List.of(1080, 720, 360)) //해상도 후보
                            .build());
                })//여기서 발생 가능한 예외 IllegalArgumentException
                //비디오 인코딩 시작
                .flatMap(encodingService::encodeVideo) //여기서 발생 가능한 예외 : InvalidInputValueException, IllegalArgumentException , KernelProcessException
                //썸네일 인코딩 시작
                .flatMap(encodingService::encodeThumbnail)//여기서 발생 가능한 예외 : InvalidInputValueException, IllegalArgumentException , KernelProcessException
                //인코딩 메서드에서 IllegalStateException 발생은 정상적인 경우가 아님. 올바른 인자를 전달하지 않았을 경우 생성 - 로직상 잘못이 있을 수 있음
                .onErrorResume(IllegalArgumentException.class, e -> Mono.error(new LocalSystemException(HttpStatus.INTERNAL_SERVER_ERROR,Collections.emptyList(),e)))
                //정상적인 응답 작성
                .then(ServerResponse.ok().build())
                //예외 처리
                .onErrorResume(ErrorResponse::commonExceptionHandler);
    }





}
