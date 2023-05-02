package com.oj.videostreamingserver.domain.vod.component;

import com.oj.videostreamingserver.domain.vod.dto.EncodingEvent;
import org.springframework.stereotype.Component;
import org.springframework.util.Assert;
import reactor.core.publisher.Sinks;

import java.util.Map;
import java.util.Optional;
import java.util.Queue;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.stream.Collectors;

/**
 * EncodingChannel is a channel that stores encoding events.
 */
@Component
public class EncodingChannel {
    //static info
    public enum Type{VIDEO, THUMBNAIL} //작업의 타입
    private final String KEY_DELIMITER = "#OF#";
    private final int MAX_QUEUE_SIZE = 1000;


    private final Queue<String> deleteQueue = new ConcurrentLinkedQueue<>();
    private final Map<String, EncodingEvent> encodingEvents = new ConcurrentHashMap<>();

    public String keyResolver(UUID videoId, Type jobType){
        return keyResolver(videoId.toString(), jobType);
    }
    public String keyResolver(String videoId, Type jobType){
        return jobType.toString() + KEY_DELIMITER + videoId.toString();
    }




    /**
     * 중계 중인 싱크를 가져오는 메서드
     * @param key 중계 키
     * @return 인코딩 싱크, 만약 없는 key 면 null
     */
    public Optional<Sinks.Many<String>> getSink(String key){
        EncodingEvent encodingEvent = encodingEvents.get(key);
        if (encodingEvent != null){
            return Optional.of(encodingEvent.getSink());
        }
        return Optional.empty();
    }





    //등록 관련 메서드

    /**
     * register new encoding event
     * @param videoId video id
     * @param jobType job type
     * @return registered encoding event
     * @throws IllegalArgumentException if there is already registered event
     */
    public String registerEvent(UUID videoId, Type jobType) throws IllegalArgumentException{
        Assert.isTrue(!isRegistered(keyResolver(videoId,jobType)), "이미 등록된 이벤트입니다.");
        String key = keyResolver(videoId, jobType);
        EncodingEvent encodingEvent = new EncodingEvent(Sinks.many().multicast().directBestEffort());
        encodingEvents.put(key,encodingEvent);
        return key;
    }


    //이벤트 종료 처리 관련 메서드

    public synchronized void reportFinish(String key){
        EncodingEvent encodingEvent = encodingEvents.get(key);
        if (encodingEvent != null && (encodingEvent.getStatus() == EncodingEvent.Status.READY || encodingEvent.getStatus() == EncodingEvent.Status.RUNNING)){
            encodingEvent.getSink().tryEmitComplete();
            encodingEvents.remove(key);
            registerToDeleteQueue(key);
        }
    }

    //에러 발생 : error occurred
    public synchronized void reportError(String key, Throwable throwable){
        EncodingEvent encodingEvent = encodingEvents.get(key);
        if (encodingEvent != null && (encodingEvent.getStatus() == EncodingEvent.Status.READY || encodingEvent.getStatus() == EncodingEvent.Status.RUNNING)){
            encodingEvent.getSink().tryEmitError(throwable);
            encodingEvent.getSink().tryEmitComplete();
            encodingEvents.remove(key);
            registerToDeleteQueue(key);
        }
    }

    public void reportRunning(String key){
        EncodingEvent encodingEvent = encodingEvents.get(key);
        if (encodingEvent != null && encodingEvent.getStatus() == EncodingEvent.Status.READY){
            encodingEvents.get(key).setStatus(EncodingEvent.Status.RUNNING);
        }
    }

    public boolean isRegistered(String key){
        return encodingEvents.containsKey(key);
    }

    /**
     * get status of encoding event
     * @param key encoding event key
     * @return status of encoding event
     * @throws IllegalArgumentException if there is no event
     */
    public EncodingEvent.Status getStatus(String key) throws IllegalArgumentException{
        EncodingEvent encodingEvent = encodingEvents.get(key);
        if (encodingEvent != null){
            return encodingEvents.get(key).getStatus();
        } else {
            //deleteQueue에 있으면
            if (deleteQueue.contains(key)){
                return EncodingEvent.Status.FINISHED;
            } else {
                throw new IllegalArgumentException("존재하지 않는 이벤트입니다.");
            }
        }
    }






    // 삭제 관련 메서드

    protected void registerToDeleteQueue(String key){
        if (encodingEvents.containsKey(key) && !deleteQueue.contains(key)){
            //삭제 큐에 추가
            deleteQueue.add(key);

            //만약 삭제 큐가 최대 크기를 넘어가면 오래된 것 부터 60%만 삭제
            if (deleteQueue.size() > MAX_QUEUE_SIZE) {
                //동시성 처리
                synchronized (this) {
                    if (deleteQueue.size() > MAX_QUEUE_SIZE) {
                        //삭제 큐가 최대 크기를 넘어가면 오래된 것 부터 60%만 삭제
                        int deleteCount = (int) (MAX_QUEUE_SIZE * 0.6);
                        String deleteKey;
                        for (int i = 0; i < deleteCount; i++) {
                            deleteKey = deleteQueue.poll();
                            encodingEvents.remove(deleteKey); //map에서 제거
                        }
                    }
                }
            }
            //종료
        }
    }
}
