package com.oj.videostreamingserver.domain.vod.service;


import com.oj.videostreamingserver.global.error.exception.InvalidInputValueException;
import com.oj.videostreamingserver.global.error.exception.LocalSystemException;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.DataBufferUtils;
import org.springframework.http.HttpStatus;
import org.springframework.http.codec.multipart.FilePart;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.io.File;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.List;

@Service
public class FileService {

    /**
     * FilePart를 저장해주는 오퍼레이터
     * @param filePart 저장할 파일
     * @param savePath 저장할 경로
     * @return 저장된 파일
     * @throws LocalSystemException 파일 저장에 실패했을 경우
     * @implNote 작업 자체는 블로킹 작업이나, subscribeOn을 통해 boundedElastic 스레드에서 작업을 수행한다.
     */
    public Mono<Void> saveFilePart(FilePart filePart, Path savePath) throws LocalSystemException{
        return Mono.defer(() -> {
                    //부모 경로를 만든다
                    Path pathParent = savePath.getParent();
                    if (!pathParent.toFile().exists()){
                        pathParent.toFile().mkdirs();
                    }
                    //파일을 저장한다.
                    Flux<DataBuffer> content = filePart.content();
                    return DataBufferUtils.write(content, savePath, StandardOpenOption.CREATE);
                })
                .onErrorResume(e -> {
                    //파일 저장에 실패했을 경우
                    return Mono.error(new LocalSystemException(HttpStatus.INTERNAL_SERVER_ERROR, List.of(savePath.toString()), e));
                })
                .subscribeOn(Schedulers.boundedElastic())
                .publishOn(Schedulers.parallel());
    }

    public Mono<Void> deleteFile(Path filePath) throws IllegalArgumentException{
        return Mono.just(filePath.toFile())
                .filter(file -> file.exists())
                .switchIfEmpty(Mono.defer(() -> {throw new IllegalArgumentException();}))
                .flatMap(file -> file.delete() ? Mono.just(file) : Mono.defer(() -> {throw new IllegalArgumentException();}))
                .subscribeOn(Schedulers.boundedElastic())
                .publishOn(Schedulers.parallel())
                .then();
    }
}
