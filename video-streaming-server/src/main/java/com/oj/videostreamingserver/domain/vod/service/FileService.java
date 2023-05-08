package com.oj.videostreamingserver.domain.vod.service;


import com.oj.videostreamingserver.global.error.exception.InvalidInputValueException;
import com.oj.videostreamingserver.global.error.exception.LocalSystemException;
import org.apache.commons.io.FileUtils;
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
                });
    }

    public Mono<Void> deleteFile(Path filePath) throws LocalSystemException{
        return Mono.just(filePath.toFile())
                .filter(File::exists)
                .flatMap(file -> Mono.just(FileUtils.deleteQuietly(file)))
                .filter(result -> result)
                .switchIfEmpty(Mono.defer(() -> {throw new LocalSystemException(HttpStatus.INTERNAL_SERVER_ERROR,List.of(filePath.toString()),null);}))
                .then();
    }
}
