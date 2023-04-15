package com.oj.videostreamingserver.domain.vod.api;

import com.oj.videostreamingserver.domain.vod.dto.OriginalVideoPostResponse;
import com.oj.videostreamingserver.domain.vod.util.VideoFileUtil;
import com.oj.videostreamingserver.global.error.exception.BadRequestException;
import com.oj.videostreamingserver.global.util.CryptoUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.codec.multipart.FilePart;
import org.springframework.util.MimeType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

import java.io.File;

@RestController
@RequiredArgsConstructor
public class VodApi {

    private final CryptoUtil cryptoUtil;

    @PostMapping("/media")
    public Mono<OriginalVideoPostResponse> postVideo(@RequestPart("video")Mono<FilePart> videoFilePart){
        return videoFilePart
                //컨텐츠 타입 체크
                .filter(fp->fp.headers().getContentType() != null)
                .filter(fp->fp.headers().getContentType().toString().startsWith("video/"))
                .switchIfEmpty(Mono.error(new BadRequestException("invalid video")))
                //컨텐츠를 저장합니다.
                .flatMap(VideoFileUtil::createTempFile) //Mono<File>
                //파일 패스를 암호화해서 전달합니다.
                .map(file -> cryptoUtil.encrypt(file.getPath())) //Mono<String>
                //응답 객체를 만듭니다.
                .map(OriginalVideoPostResponse::new)
                .log();
    }
}
