package com.oj.videostreamingserver.domain.vod.dto.request;

import com.oj.videostreamingserver.global.error.exception.InvalidInputValueException;
import lombok.Builder;
import lombok.Getter;
import org.springframework.http.codec.multipart.FilePart;
import org.springframework.lang.Nullable;
import org.springframework.web.reactive.function.server.ServerRequest;
import reactor.core.publisher.Mono;

import java.util.UUID;
import javax.validation.constraints.NotNull;

/**
 * 비디오 포스팅 요청 바디
 */
@Getter
public class VideoPostRequest {
    //클라이언트가 바디로 보내주는 정보
    @NotNull
    private FilePart videoFile;
    @Nullable
    private FilePart thumbnail;

    private UUID videoId;



    @Builder
    public VideoPostRequest(FilePart videoFile, @Nullable FilePart thumbnail, UUID videoId) {
        this.videoFile = videoFile;
        this.thumbnail = thumbnail;
        this.videoId = videoId;
    }




    /**
     * 서버 요청을 받아서 Mono<VodPostRequestBody>로 변환하는 메서드 <br>
     * 만약 video가 없으면, {@link InvalidInputValueException}을 {@link Mono}에 담아서 리턴한다. <br>
     * @param serverRequest 서버 요청
     * @return Mono<VodPostRequestBody>
     */
    public static Mono<VideoPostRequest> from(ServerRequest serverRequest){
        return Mono.just(serverRequest)
                //바디 필드 체크
                .flatMap(request -> request.multipartData()
                        //필수 필드 체크 - video 필드와 videoId 필드가 있는지 확인
                        .filter(multiValueMap -> multiValueMap.getFirst("video") instanceof FilePart)
                        .switchIfEmpty(Mono.defer(()->Mono.error(new InvalidInputValueException("mandatory field","","video field is mandatory, but not exist or type unmatched(needed = FilePart)"))))
                        //optional 필드 체크 - thumbnail 필드가 있는지 확인
                        //thumbnail 필드가 있으면 FilePart 인지 확인
                        .filter(multiValueMap -> !multiValueMap.containsKey("thumbnail") || multiValueMap.getFirst("thumbnail") instanceof FilePart)
                        .switchIfEmpty(Mono.defer(() -> Mono.error(new InvalidInputValueException("optional field","","thumbnail field exist, but type unmatched (needed = FilePart)"))))
                        .then(Mono.just(request)
                ))
                //dto 생성
                .flatMap(request -> {
                    String videoIdField = request.pathVariable("videoId");
                    UUID videoId;
                    try {
                        videoId = UUID.fromString(videoIdField);
                    } catch (IllegalArgumentException e){
                        return Mono.error(new InvalidInputValueException("path variable","videoId", "videoId is not UUID format"));
                    }

                    return request.multipartData()
                            .flatMap(multiValueMap -> {
                                //필수 필드
                                FilePart videoFile = (FilePart) multiValueMap.getFirst("video");
                                //optional 필드
                                FilePart thumbnail;
                                if (multiValueMap.containsKey("thumbnail")) {
                                    thumbnail = (FilePart) multiValueMap.getFirst("thumbnail");
                                } else {
                                    thumbnail = null;
                                }
                                return Mono.just(new VideoPostRequest(videoFile, thumbnail, videoId));
                            });
                });
    }

}
