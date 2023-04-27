package com.oj.videostreamingserver.domain.vod.component;

import com.oj.videostreamingserver.domain.vod.dto.EncodingEvent;
import org.springframework.stereotype.Component;
import org.springframework.util.Assert;
import reactor.core.publisher.Sinks;

import java.util.Map;
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
    private final String KEY_DELIMITER = "!";
    private final int MAX_QUEUE_SIZE = 1000;


    private final Queue<String> deleteQueue = new ConcurrentLinkedQueue<>();
    private final Map<String, EncodingEvent> encodingEvents = new ConcurrentHashMap<>();

    private String keyResolver(UUID videoId, Type jobType){
        return videoId.toString() + KEY_DELIMITER + jobType.toString();
    }


    public EncodingEvent getEvent(String key) {
        return encodingEvents.get(key);
    }

    public String registerEvent(UUID videoId, Type jobType){
        return registerEvent(videoId, jobType, new EncodingEvent(Sinks.many().multicast().directBestEffort()));
    }
    public String registerEvent(UUID videoId, Type jobType, EncodingEvent encodingEvent){
        Assert.isTrue(!isRegistered(keyResolver(videoId,jobType)), "이미 등록된 이벤트입니다.");
        String key = keyResolver(videoId, jobType);
        encodingEvents.put(key,encodingEvent);
        return key;
    }

    public void registerToDeleteQueue(String key){
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
    public void removeEvent(String key){
        if (encodingEvents.containsKey(key)){
            //맵에서 제거
            encodingEvents.remove(key);
            //삭제 큐에 추가
            deleteQueue.add(key);

            if (deleteQueue.size() > MAX_QUEUE_SIZE) {
                //cleanup bot
                synchronized (this) {
                    if (deleteQueue.size() > MAX_QUEUE_SIZE) {
                        //삭제 큐가 최대 크기를 넘어가면 오래된 것 부터 60%만 삭제
                        int deleteCount = (int) (MAX_QUEUE_SIZE * 0.6);
                        for (int i = 0; i < deleteCount; i++) {
                            deleteQueue.poll();
                        }
                    }
                }
            }

        }
        return;
    }

    public boolean isRegistered(String key){
        return encodingEvents.containsKey(key);
    }

    public boolean isFinished(UUID videoId, Type jobType){
        EncodingEvent encodingEvent = encodingEvents.get(keyResolver(videoId, jobType));
        if (encodingEvent != null){
            return encodingEvents.get(keyResolver(videoId, jobType)).getStatus() == EncodingEvent.Status.FINISHED;
        }
        return true;
    }

    public EncodingEvent.Status getStatus(UUID videoId, Type jobType){
        EncodingEvent encodingEvent = encodingEvents.get(keyResolver(videoId, jobType));
        if (encodingEvent != null){
            return encodingEvents.get(keyResolver(videoId, jobType)).getStatus();
        }
        return EncodingEvent.Status.FINISHED;
    }
}
