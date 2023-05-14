package com.oj.videostreamingserver.domain.vod.component;

import com.oj.videostreamingserver.domain.vod.dto.domain.EncodingEvent;
import org.springframework.util.Assert;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.publisher.Sinks;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * EncodingChannel is a channel that stores encoding events.
 */
public class EncodingChannel {
    //static info
    public enum Type{VIDEO, THUMBNAIL} //작업의 타입
    private final String KEY_DELIMITER = "#OF#";

    private final Map<String, EncodingEvent<String>> encodingEvents = new ConcurrentHashMap<>();

    public String keyResolver(UUID videoId, Type jobType){
        return keyResolver(videoId.toString(), jobType);
    }
    public String keyResolver(String videoId, Type jobType){
        return jobType.toString() + KEY_DELIMITER + videoId.toString();
    }

    public Optional<EncodingEvent<String>> getEncodingEvent(final UUID videoId, final Type jobType){
        String key = keyResolver(videoId, jobType);
        return getEncodingEvent(key);
    }

    public Optional<EncodingEvent<String>> getEncodingEvent(final String key){
        return Optional.ofNullable(encodingEvents.get(key));
    }

    public void registerEvent(final UUID videoId, final Type jobType, EncodingEvent<String> encodingEvent) throws IllegalArgumentException {
        this.encodingEvents.put(keyResolver(videoId,jobType), encodingEvent);
    }
    public void removeEncodingEvent(final EncodingEvent<String> value){
        //찾아서 map에서 삭제한다.
        encodingEvents.entrySet().removeIf(entry -> entry.getValue().equals(value));
    }


    public boolean contains(UUID videoId , Type jobType){
        return encodingEvents.containsKey(keyResolver(videoId, jobType));
    }

    public Map<Type, EncodingEvent.Status> selectByVideoId(UUID videoId){
        Map<Type, EncodingEvent.Status> result = new HashMap<>();
        for (Type type : Type.values()){
            String key = keyResolver(videoId, type);
            EncodingEvent<String> event;
            if ((event = encodingEvents.get(key)) != null){
                result.put(type, event.getStatus());
            }
        }
        return result;
    }

    static class Fallbacks{

        public static <R> Mono<R> defaultFallback(){
            return Mono.<R>empty();
        }

    }
}
