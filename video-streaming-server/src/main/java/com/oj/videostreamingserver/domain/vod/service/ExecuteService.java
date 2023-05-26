package com.oj.videostreamingserver.domain.vod.service;

import com.oj.videostreamingserver.domain.vod.component.EncodingChannel;
import com.oj.videostreamingserver.domain.vod.dto.domain.EncodingEvent;
import com.oj.videostreamingserver.global.error.exception.KernelProcessException;
import lombok.Builder;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import reactor.core.Disposable;
import reactor.core.publisher.Flux;
import reactor.core.publisher.FluxSink;
import reactor.core.publisher.SignalType;
import reactor.core.publisher.Sinks;
import reactor.core.scheduler.Schedulers;


import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.List;
import java.util.UUID;
import java.util.function.BiConsumer;
import java.util.function.Function;

@Slf4j
@Component
@RequiredArgsConstructor
public class ExecuteService {

    private final EncodingChannel encodingChannel;


    /**
     * 프로세스를 실행시키고 로그를 방출하는 flux를 반환
     *
     * @param processBuilder 실행시킬 프로세스 빌더
     * @return 프로세스를 실행시키고 로그를 방출하는 flux
     */
    @Builder(builderMethodName = "executeAndEmitLogBuilder")
    public Disposable executeAndEmitLog(final ProcessBuilder processBuilder,
                                        BiConsumer<String, FluxSink<String>> logLineConsumer,
                                        EncodingEvent<String> encodingEvent,
                                        UUID videoId,
                                        EncodingChannel.Type type) {
        //중계 이벤트 등록
        encodingChannel.registerEvent(videoId, type, encodingEvent);
        processBuilder.redirectErrorStream(true); //에러 스트림을 표준 출력으로 합침

        return Flux.<String>create(emitter -> {
            Process process = null;
            try {
                process = processBuilder.start();
            } catch (IOException e) {
                emitter.error(new KernelProcessException("ffmpeg", List.of(processBuilder.command().toString()),null));
            }
            try (InputStream inputStream = process.getInputStream();
                BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream))) {
                //파싱해서 퍼센트 단위로 전달
                reader.lines()
                        .forEach(line -> {
                            logLineConsumer.accept(line, emitter);
                        });

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
                .doOnNext(encodingEvent::reportNext)
                .doOnError(encodingEvent::reportError)
                .doOnComplete(encodingEvent::reportComplete)
                .doFinally(signalType -> encodingChannel.removeEncodingEvent(encodingEvent))
                .subscribeOn(Schedulers.boundedElastic())
                .subscribe();
    }

}
