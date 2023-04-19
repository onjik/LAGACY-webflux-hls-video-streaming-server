package com.oj.videostreamingserver.domain.vod.handler;

import com.oj.videostreamingserver.domain.vod.domain.DraftVideo;
import com.oj.videostreamingserver.domain.vod.dto.OriginalVideoPostResponse;
import com.oj.videostreamingserver.domain.vod.repository.DraftVideoRepository;
import com.oj.videostreamingserver.domain.vod.service.FileService;
import com.oj.videostreamingserver.global.error.ErrorResponse;
import com.oj.videostreamingserver.global.error.exception.DbException;
import com.oj.videostreamingserver.global.error.exception.InvalidInputValueException;
import com.oj.videostreamingserver.global.error.exception.LocalFileException;
import com.oj.videostreamingserver.global.util.FileUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.http.codec.multipart.FilePart;
import org.springframework.http.codec.multipart.Part;
import org.springframework.stereotype.Component;
import org.springframework.transaction.reactive.TransactionalOperator;
import org.springframework.web.reactive.function.server.ServerRequest;
import org.springframework.web.reactive.function.server.ServerResponse;
import reactor.core.publisher.Mono;

import java.io.File;
import java.nio.file.InvalidPathException;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

/**
 * POST /media : 원본 비디오 포스팅 용 API 핸들러
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class VodPostHandler {

    //constants
    @Value("${volume.media}")
    private String MEDIA_VOLUME_ROOT;
    private final String PATH_SEPARATOR = File.pathSeparator;

    //spring beans
    private final TransactionalOperator transactionalOperator;

    private final FileService fileService;
    private final DraftVideoRepository draftVideoRepository;


    /**
     * POST /media : 원본 비디오 포스팅 용 API 핸들러
     * @param request ServerRequest
     * @return ServerResponse 긍정 응답의 경우 body : {@link OriginalVideoPostResponse} 부정 응답의 경우 body : {@link ErrorResponse}
     *
     * @implNote 다음 예외들은 스트림 내부적으로 발생하고 처리됩니다.
     * {@link InvalidInputValueException} 요청에 문제가 있음 : video field, content-type, video filename ,
     * {@link LocalFileException} 저장될 파일의 경로를 지정하는 도중 문제가 생겼습니다. : path mkdir, 경로가 올바르지 않음, 구분자가 잘못됨 등,
     * {@link IllegalStateException} partFile 이 파일이 아닙니다. ,
     * {@link org.springframework.dao.OptimisticLockingFailureException} 논리적으로 발생하지 않는다고 생각하여 처리하지 않음. 오직 INSERT 를 위해서만 save 를 호출하기 떄문
     * {@link org.springframework.dao.DataIntegrityViolationException} DB의 데이터 무결성 옵션을 위반했습니다. : 외래키, not null 등
     *
     */
    public Mono<ServerResponse> postVideo(ServerRequest request) {
        long channelId = 1; //임시로 구현
        return this.checkRequestValidation(request)//Mono<FilePart>
                //메인 로직
                .as(transactionalOperator::transactional)
                .flatMap(fileService::saveVideoToDraft) //Mono<DraftVideo>
                .flatMap(file -> {
                    try {
                        return draftVideoRepository.save(new DraftVideo(file.getAbsolutePath(), channelId));
                    } catch (Exception e) {
                        //DB 저장중 예외 발생시 로컬 파일 롤백 처리
                        file.delete();
                        return Mono.error(new DbException(e));
                    }})
                //응답 처리
                .flatMap(draftVideo ->
                        ServerResponse
                                .ok()
                                .contentType(MediaType.APPLICATION_JSON)
                                .bodyValue(new OriginalVideoPostResponse(draftVideo.getId()))
                )
                //예외 처리
                .onErrorResume(ErrorResponse::commonExceptionHandler);
    }

    /**
     * check client request is valid and extract FilePart from Multipart form data
     * @param serverRequest client request
     * @return return Mono<FilePart> if client Request is valid, <br>
     * return Mono.error({@link InvalidInputValueException}) if request is not valid.
     */
    protected Mono<FilePart> checkRequestValidation(ServerRequest serverRequest){
        return Mono.defer(() -> Mono.just(serverRequest)
                //content -type 체크
                .filter(request -> MediaType.MULTIPART_FORM_DATA.isCompatibleWith(
                        request.headers().contentType().orElse(null)))
                .switchIfEmpty(Mono.error(()->
                        new InvalidInputValueException(
                                "Content-Type Header","","Content-Type Must Be multipart/form-data")))
                .flatMap(request -> Mono.defer(request::multipartData))//Mono<MultiValueMap>
                //video 필드 체크
                .filter(multiMap -> multiMap.getOrDefault("video", Collections.emptyList()).size() == 1)
                .flatMap(multimap -> Mono.just(multimap.getFirst("video")))
                .ofType(FilePart.class)//Mono<FilePart>
                .switchIfEmpty(Mono.error(new InvalidInputValueException("video", "", "Exactly one video file is required")))
                //multipart content-type 헤더 체크
                .filter(fp -> fp.headers().containsKey("Content-Type") && fp.headers().getFirst("Content-Type").startsWith("video/"))
                .switchIfEmpty(Mono.error(new InvalidInputValueException("Multipart Content-Type","","Content-Type that start with video/ is required")))
        );
    }


}
