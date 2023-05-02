package com.oj.videostreamingserver.domain.vod.service;


import com.oj.videostreamingserver.global.error.exception.InvalidInputValueException;
import com.oj.videostreamingserver.global.error.exception.LocalSystemException;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.DataBufferUtils;
import org.springframework.http.HttpStatus;
import org.springframework.http.codec.multipart.FilePart;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.io.File;
import java.io.IOException;
import java.nio.channels.AsynchronousFileChannel;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.Collections;
import java.util.List;

@Service
public class FileService {

    public Mono<Void> saveFilePart(FilePart filePart, Path path) throws InvalidInputValueException {
        return Mono.defer(() -> {
                    Path parent = path.getParent();
                    if (!parent.toFile().exists()){
                        parent.toFile().mkdirs();
                    }
                    Flux<DataBuffer> content = filePart.content();
                    return DataBufferUtils.write(content, path, StandardOpenOption.CREATE);
                })
                .subscribeOn(Schedulers.boundedElastic())
                .onErrorResume(IllegalStateException.class, e -> Mono.error(new InvalidInputValueException("videoField","","파일이 아닙니다.")));
    }

    public Mono<Void> deleteDirectory(Path filePath){
        File file = filePath.toFile();
        return Mono.<Void>fromCallable(() -> {
            if(file.exists()){
                if (file.delete()){
                    return null;
                } else {
                    //파일이 삭제가 안됬을 경우 http status 500
                    throw new LocalSystemException(HttpStatus.INTERNAL_SERVER_ERROR, List.of(file.getAbsolutePath()), null);
                }
            } else {
                //파일이 존재하지 않을 경우 http status 404
                throw new LocalSystemException(HttpStatus.NOT_FOUND, List.of(file.getAbsolutePath()), null);
            }
        }).subscribeOn(Schedulers.boundedElastic());
    }
}
