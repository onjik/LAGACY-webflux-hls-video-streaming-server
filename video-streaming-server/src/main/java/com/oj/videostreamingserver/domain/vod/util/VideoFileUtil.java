package com.oj.videostreamingserver.domain.vod.util;

import com.oj.videostreamingserver.global.error.ErrorCode;
import com.oj.videostreamingserver.global.error.ErrorResponse;
import com.oj.videostreamingserver.global.error.exception.BadRequestException;
import org.springframework.http.codec.multipart.FilePart;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.io.File;
import java.util.UUID;

public class VideoFileUtil {

    public static String SYS_TMP_PATH = System.getProperty("java.io.tmpdir");

    public static Mono<File> createTempFile(FilePart filePart){
        File file = new File(SYS_TMP_PATH, UUID.randomUUID() + extensionOf(filePart.filename()));
        return filePart.transferTo(file)
                .then(Mono.just(file));
    }


    public static String extensionOf(String fileName) throws IllegalArgumentException{
        int lastIndex = fileName.lastIndexOf('.');
        if (lastIndex != -1 && lastIndex < fileName.length() - 1) {
            return fileName.substring(lastIndex);
        } else {
            throw new IllegalArgumentException("filename ( " + fileName + " ) has no extension");
        }
    }
}
