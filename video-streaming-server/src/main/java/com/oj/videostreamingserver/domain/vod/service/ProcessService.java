package com.oj.videostreamingserver.domain.vod.service;

import com.oj.videostreamingserver.domain.vod.component.EncodingChannel;
import com.oj.videostreamingserver.domain.vod.dto.domain.EncodingEvent;
import com.oj.videostreamingserver.global.error.exception.KernelProcessException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Sinks;


import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.List;
import java.util.UUID;

@Slf4j
@Component
@RequiredArgsConstructor
public class ProcessService {

    private final EncodingChannel encodingChannel;


    /**
     * 프로세스를 실행시키고 로그를 방출하는 flux를 반환
     *
     * @param processBuilder 실행시킬 프로세스 빌더
     * @return 프로세스를 실행시키고 로그를 방출하는 flux
     */
    public Flux<String> executeAndEmitLog(final ProcessBuilder processBuilder, UUID videoId, EncodingChannel.Type type) {
        //중계 이벤트 등록
        EncodingEvent<String> encodingEvent = encodingChannel.registerEvent(videoId, type);
        Sinks.Many<String> sink = encodingEvent.getSink();
        processBuilder.redirectErrorStream(true); //에러 스트림을 표준 출력으로 합침
        //processBuilder를 실행시키고 inputstream을 한줄씩 읽어서 방출하는 flux

        return Flux.<String>create(emitter -> {
            Process process = null;
            try {
                process = processBuilder.start();
            } catch (IOException e) {
                emitter.error(new KernelProcessException("ffmpeg", List.of(processBuilder.command().toString()),null));
            }
            try (InputStream inputStream = process.getInputStream();
                BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream))) {
                //인풋 스트림을 next로 중계
                String line;
                while ((line = reader.readLine()) != null){
                    emitter.next(line);
                }

                if (process.waitFor() != 0){
                    new BufferedReader(new InputStreamReader(process.getErrorStream())).lines().forEach(log::error);
                    emitter.error(new KernelProcessException("ffmpeg", List.of(processBuilder.command().toString()),null));
                } else {
                    emitter.complete();
                }
            } catch (Throwable e) {
                emitter.error(e);
            }
        })

                .doOnSubscribe(subscription -> encodingEvent.reportRunning())
                .doOnNext(sink::tryEmitNext)
                .doOnError(e -> {
                    sink.tryEmitError(e);
                    sink.tryEmitComplete();
                })
                .onErrorResume(e -> encodingEvent.getFallback())
                .doOnComplete(() -> {
                    sink.tryEmitComplete();
                    encodingChannel.removeEncodingEvent(encodingChannel.keyResolver(videoId, type));
                });
    }

}
