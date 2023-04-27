package com.oj.videostreamingserver.domain.vod.component;

import com.oj.videostreamingserver.domain.vod.dto.EncodingEvent;
import org.springframework.stereotype.Component;
import org.springframework.util.Assert;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.concurrent.*;

@Component
public class IndependentExecutor {
    private ExecutorService executor;

    @PostConstruct
    public void init(){
        executor = Executors.newFixedThreadPool(2); //독립적인 작업을 위한 쓰레드풀
    }

    @PreDestroy
    public void destroy(){
        executor.shutdown();
    }

    /**
     * 작업 제출
     * @param runnable 실행할 작업
     * @throws RejectedExecutionException if the task cannot be scheduled for execution
     * @throws NullPointerException if the task is null
     */
    public void submit(Runnable runnable) {
        executor.submit(runnable);
    }


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
        executor.submit(() -> {
            EncodingEvent event = encodingChannel.getEvent(broadCastKey);
            try {
                Process process = processBuilder.start();
                event.setStatus(EncodingEvent.Status.RUNNING); //작업 중으로 바꿈

                //커널의 출력 가져오기
                BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
                String line;

                while (process.isAlive()){
                    while ((line = reader.readLine()) != null){
                        event.getSink().tryEmitNext(line); //중계
                    }
                }
                //종료
                if (process.waitFor() == 0) {
                    event.setStatus(EncodingEvent.Status.FINISHED);
                } else {
                    event.setStatus(EncodingEvent.Status.ERROR);
                }
                encodingChannel.registerToDeleteQueue(broadCastKey);

            } catch (Throwable e) {
                event.setStatus(EncodingEvent.Status.ERROR);
                encodingChannel.registerToDeleteQueue(broadCastKey);

            }
            //정상적이던 아니던, 종료
        });
    }

    <T> Future<T> submit(Callable<T> callable){
        return executor.submit(callable);
    }
}
