package com.oj.videostreamingserver.domain.vod.dto.request;

import com.oj.videostreamingserver.global.error.exception.InvalidInputValueException;
import lombok.Getter;
import org.springframework.http.codec.multipart.FilePart;
import org.springframework.web.reactive.function.server.ServerRequest;
import reactor.core.publisher.Mono;

import java.util.UUID;

@Getter
public class ThumbnailPatchRequest {
    private UUID videoId;
    private FilePart thumbnail;

    public ThumbnailPatchRequest(UUID videoId, FilePart thumbnail) {
        this.videoId = videoId;
        this.thumbnail = thumbnail;
    }

    public static Mono<ThumbnailPatchRequest> from(ServerRequest request){
        UUID videoId = UUID.fromString(request.pathVariable("videoId"));
        return request.multipartData()
                .filter(multiValueMap -> multiValueMap.getFirst("thumbnail") instanceof FilePart)
                .filter(multiValueMap -> multiValueMap.getFirst("thumbnail").headers().getContentType().getType().equals("image"))
                .switchIfEmpty(Mono.defer(()->Mono.error(new InvalidInputValueException("mandatory field","","thumbnail field is mandatory, but not exist or type unmatched(needed = FilePart)"))))
                .flatMap(multiValueMap -> Mono.just((FilePart)multiValueMap.getFirst("thumbnail")))
                .flatMap(thumbnail -> Mono.just(new ThumbnailPatchRequest(videoId, thumbnail)));

    }
}
