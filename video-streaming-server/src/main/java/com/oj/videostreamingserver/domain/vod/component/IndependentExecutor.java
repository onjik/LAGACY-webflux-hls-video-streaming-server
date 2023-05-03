package com.oj.videostreamingserver.domain.vod.component;

import com.oj.videostreamingserver.global.error.exception.KernelProcessException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.util.Assert;
import reactor.core.publisher.Mono;
import reactor.core.publisher.Sinks;
import reactor.core.scheduler.Schedulers;


import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.List;
import java.util.concurrent.*;

@Component
@Slf4j
public class IndependentExecutor {

    /**
     * 아래와 같은 작업을 진행합니다. <br>
     * 반드시 중계 키를 등록한 후 사용해야 합니다. <br>
     * 1. 주어진 processBuilder 를 독립된 쓰레드 풀에서 실행시킨다. <br>
     * 2. 주어진 중계키와 채널을 통해 커널의 출력을 중계한다. <br>
     * 3. 완료되었을 경우 complete 시그널을 중계하고, 채널의 제거 큐에 추가한다. <br>
     * 4. 예외가 발생했을 경우 error 시그널 중계하고, 채널의 제거큐에 추가한다.
     * @param processBuilder 실행할 프로세스 빌더
     * @param encodingChannel 중계할 채널
     * @param broadCastKey 중계할 키
     * @throws IllegalArgumentException 만약 주어진 중계키가 등록이 안된 경우
     */
    public void executeAndBroadCast(ProcessBuilder processBuilder, EncodingChannel encodingChannel, String broadCastKey) throws IllegalArgumentException{
        Assert.isTrue(encodingChannel.isRegistered(broadCastKey), "중계 키가 등록되지 않았습니다.");
        Assert.notNull(processBuilder, "processBuilder 는 null 일 수 없습니다.");
        Assert.notNull(encodingChannel, "encodingChannel 은 null 일 수 없습니다.");
        processBuilder.redirectErrorStream(true); //에러 스트림을 표준 출력으로 합침
        //아래는 디버그용
//        processBuilder.redirectOutput(ProcessBuilder.Redirect.INHERIT);
        log.debug("processBuilder : {}", processBuilder.command());

        Callable<Process> callable = () -> {
            Process process = null;
            encodingChannel.reportRunning(broadCastKey);
            //프로세스 시작
            try {
                process = processBuilder.start();
                encodingChannel.reportRunning(broadCastKey);
            } catch (IOException e) {
                encodingChannel.reportError(broadCastKey,e);
                if (process != null) {
                    process.destroy();
                }
                List<String> command = processBuilder.command();
                throw new KernelProcessException(command.get(0), List.of(command.toString()),e);
            }

            //출력 부분
            String line;
            try (InputStream inputStream = process.getInputStream();BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream))){
                Sinks.Many<String> sink = encodingChannel.getSink(broadCastKey).orElseThrow(() -> new IllegalArgumentException("중계 키가 등록되지 않았습니다."));
                //좀더 효율적으로 하기 위해 만약 프로세스가 종료되지 않고 reader에 준비가 되어있지 않으면 쓰레드 수행시간을 양보함
                while (process.isAlive()){
                    if (!reader.ready()){
                        Thread.yield();
                    }else{
                        sink.tryEmitNext(reader.readLine());
                    }
                }

                //끝났으면
                if (process.exitValue() != 0) {
                    log.error("process {} --> exit with {}",broadCastKey,process.exitValue());
                    encodingChannel.reportError(broadCastKey,new KernelProcessException(processBuilder.command().get(0), processBuilder.command(),null));
                } else {
                    log.debug("process {} --> exit with {}",broadCastKey,process.exitValue());
                    encodingChannel.reportFinish(broadCastKey);
                }
            } catch (Throwable e) {
                log.error("unhandled error",e);
                encodingChannel.reportError(broadCastKey,e);
            } finally {
                process.destroy();
            }
            return process;
        };

        Mono<Process> processMono = Mono.fromCallable(callable);

        /*
        본래 쓰레드와는 별개로, boundedElastic 쓰레드에서 실행
        이 쓰레드는 백그라운드에서 실행되고 종료됩니다.
         */
        processMono.subscribeOn(Schedulers.boundedElastic()).subscribe();
    }

}
