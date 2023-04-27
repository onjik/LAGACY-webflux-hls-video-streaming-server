package com.oj.videostreamingserver.domain.vod.dto;

import com.oj.videostreamingserver.global.error.exception.InvalidInputValueException;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import org.springframework.http.codec.multipart.FilePart;
import org.springframework.http.codec.multipart.FormFieldPart;
import org.springframework.http.codec.multipart.Part;
import org.springframework.lang.Nullable;
import org.springframework.web.reactive.function.server.ServerRequest;
import reactor.core.publisher.Mono;

import java.nio.file.Path;
import java.util.UUID;
import javax.validation.constraints.NotNull;

/**
 * 비디오 포스팅 요청 바디
 */
@Getter
public class VodPostRequestBody {
    //클라이언트가 바디로 보내주는 정보
    @NotNull
    private FilePart videoFile;
    @Nullable
    private FilePart thumbnail;

    private UUID videoId;



    @Builder
    public VodPostRequestBody(FilePart videoFile, @Nullable FilePart thumbnail, UUID videoId) {
        this.videoFile = videoFile;
        this.thumbnail = thumbnail;
        this.videoId = videoId;
    }

    public void setVideoId(UUID videoId) {
        this.videoId = videoId;
    }

    /**
     * Part의 타입이 clazz와 같은지 확인하는 메서드
     * @param clazz 확인할 타입
     * @param part 확인할 Part
     */
    private static void checkType(Class<? extends Part> clazz, Part part){
        if (clazz.isInstance(part)) {
            return;
        } else {
            throw new InvalidInputValueException("video field", "", "type unmatched");
        }
    }

    /**
     * 서버 요청을 받아서 Mono<VodPostRequestBody>로 변환하는 메서드
     * @param request 서버 요청
     * @return Mono<VodPostRequestBody>
     */
    public static Mono<VodPostRequestBody> monoFromServerRequest(ServerRequest request){
        return request.multipartData()
                //필드 체크 - video 필드가 있는지 확인
                .filter(multiValueMap -> multiValueMap.containsKey("video"))
                .switchIfEmpty(reactor.core.publisher.Mono.error(new InvalidInputValueException("video field","null","there is no video field")))
                //dto 생성
                .flatMap(multiValueMap -> {
                    VodPostRequestBodyBuilder builder = VodPostRequestBody.builder();
                    //video 필드가 FilePart인지 확인
                    checkType(FilePart.class,multiValueMap.getFirst("video"));
                    FilePart video = (FilePart) multiValueMap.getFirst("video");
                    builder.videoFile(video);

                    //thumbnail 필드
                    if (multiValueMap.containsKey("thumbnail")) {
                        //thumbnail 필드가 FilePart인지 확인
                        checkType(FilePart.class, multiValueMap.getFirst("thumbnail"));
                        builder.thumbnail((FilePart) multiValueMap.getFirst("thumbnail"));
                    } else {
                        builder.thumbnail(null);
                    }

                    return Mono.just(builder.build());
                });
    }

}
