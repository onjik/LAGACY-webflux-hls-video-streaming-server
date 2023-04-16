package com.oj.videostreamingserver.domain.vod.api;

import com.oj.videostreamingserver.domain.vod.domain.DraftVideo;
import com.oj.videostreamingserver.domain.vod.dto.OriginalVideoPostResponse;
import com.oj.videostreamingserver.domain.vod.exception.InvalidTargetPathException;
import com.oj.videostreamingserver.domain.vod.service.VideoFileService;
import com.oj.videostreamingserver.global.error.ErrorCode;
import com.oj.videostreamingserver.global.error.ErrorResponse;
import com.oj.videostreamingserver.global.error.FieldError;
import com.oj.videostreamingserver.global.error.exception.BadRequestException;
import com.oj.videostreamingserver.global.util.CryptoUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.codec.multipart.FilePart;
import org.springframework.transaction.reactive.TransactionalOperator;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.reactive.function.server.ServerResponse;
import reactor.core.publisher.Mono;

//@RestController
//@RequiredArgsConstructor
public class VodApi {
//
//    private final CryptoUtil cryptoUtil;
//
//    private final VideoFileService videoFileService;
//    private final TransactionalOperator transactionalOperator;
//
//    @PostMapping("/media")
//    public Mono<ServerResponse> postVideo(@RequestPart("video")Mono<FilePart> videoFilePart){
//        long channelId = 1; //임시로 구현
//        return videoFilePart
//                .log()
//                //컨텐츠 타입 체크
//                .filter(fp->fp.headers().getContentType() != null)
//                .filter(fp->fp.headers().getContentType().toString().startsWith("video/"))
//                .switchIfEmpty(Mono.error(new BadRequestException("invalid video")))
//                //트랜잭션 처리
//                .as(transactionalOperator::transactional)
//                //컨텐츠를 로컬 파일에 저장합니다.
//                .flatMap(filePart -> videoFileService.saveDraft(filePart,channelId)) //Mono<DraftVideo>
//                //확인 응답
//                .flatMap(draftVideo ->
//                        ServerResponse
//                                .ok().build()
////                                .contentType(MediaType.APPLICATION_JSON)
////                                .body(new OriginalVideoPostResponse(draftVideo.getId()), OriginalVideoPostResponse.class)
//                )
//                //예외처리
//                .onErrorResume(BadRequestException.class,e -> ErrorResponse.of(ErrorCode.INVALID_INPUT_VALUE))
//                .onErrorResume(InvalidTargetPathException.class,ErrorResponse::of)
//                .onErrorResume(Exception.class, ErrorResponse::of);
//    }
}
