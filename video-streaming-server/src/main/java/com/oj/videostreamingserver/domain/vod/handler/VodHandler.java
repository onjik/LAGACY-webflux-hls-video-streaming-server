package com.oj.videostreamingserver.domain.vod.handler;

import com.oj.videostreamingserver.domain.vod.dto.OriginalVideoPostResponse;
import com.oj.videostreamingserver.domain.vod.exception.InvalidTargetPathException;
import com.oj.videostreamingserver.domain.vod.service.VideoFileService;
import com.oj.videostreamingserver.global.error.ErrorCode;
import com.oj.videostreamingserver.global.error.ErrorResponse;
import com.oj.videostreamingserver.global.error.exception.BadRequestException;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.MediaType;
import org.springframework.http.codec.multipart.FilePart;
import org.springframework.http.codec.multipart.Part;
import org.springframework.stereotype.Component;
import org.springframework.transaction.reactive.TransactionalOperator;
import org.springframework.util.MultiValueMap;
import org.springframework.web.reactive.function.server.ServerRequest;
import org.springframework.web.reactive.function.server.ServerResponse;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Objects;

@Component
@RequiredArgsConstructor
public class VodHandler {

    private final TransactionalOperator transactionalOperator;
    private final VideoFileService videoFileService;

    public Mono<ServerResponse> postVideo(ServerRequest request) {
        long channelId = 2; //임시로 구현
        return request.multipartData()
                .log()
                //video 추출
                .flatMap(map -> {
                    Part video = map.getFirst("video");
                    if (video instanceof FilePart) {
                        return Mono.just((FilePart) video);
                    } else {
                        return Mono.empty();
                    }
                })
                //content type 체크
                .filter(fp -> Objects.requireNonNull(fp.headers().getContentType()).toString().startsWith("video/"))
                //만약 적절한 스트림이 없으면 BAD REQUEST
                .switchIfEmpty(Mono.error(new BadRequestException("invalid video")))
                //트랜잭션 처리
                .as(transactionalOperator::transactional)
                //컨텐츠를 로컬 파일에 저장합니다.
                .flatMap(filePart -> videoFileService.saveDraft(filePart, channelId)) //Mono<DraftVideo>
                //확인 응답
                .flatMap(draftVideo ->
                                ServerResponse
                                        .ok()
                                        .contentType(MediaType.APPLICATION_JSON)
                                        .bodyValue(new OriginalVideoPostResponse(draftVideo.getId()))
                )
                //예외처리 - 리펙토링 필요
                .onErrorResume(DataIntegrityViolationException.class, e -> {
                    if (e.getMessage().toUpperCase().contains("FOREIGN")) {
                        return ErrorResponse.of(ErrorCode.INVALID_INPUT_VALUE);
                    } else{
                        return ErrorResponse.of(ErrorCode.INTERNAL_SERVER_ERROR);
                    }
                })
                .onErrorResume(BadRequestException.class, e -> ErrorResponse.of(ErrorCode.INVALID_INPUT_VALUE))
                .onErrorResume(InvalidTargetPathException.class, ErrorResponse::of)
                .onErrorResume(Exception.class, ErrorResponse::of);

    }
}
