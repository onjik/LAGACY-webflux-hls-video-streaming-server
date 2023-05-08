package com.oj.videostreamingserver.domain.vod.dto.domain;

import org.reactivestreams.Publisher;
import org.springframework.util.Assert;
import reactor.core.publisher.Mono;
import reactor.core.publisher.Sinks;

public class EncodingEvent<T> {
    public enum Status {
        READY,RUNNING,COMPLETE,ERROR
    };
    private final Sinks.Many<T> sink;
    private volatile Status status;

    //예외시 실행할 폴백
    private final Mono<T> fallback;


    public EncodingEvent(Sinks.Many<T> sink, Mono<T> fallback) {
        Assert.notNull(sink, "sink must not be null");
        Assert.notNull(fallback, "fallback must not be null");
        this.sink = sink;
        this.fallback = fallback;
        this.status = Status.READY;
    }

    public Mono<T> getFallback() {
        return fallback;
    }

    public Sinks.Many<T> getSink() {
        return sink;
    }

    public Status getStatus() {
        return status;
    }

    public synchronized void reportRunning(){
        this.status = Status.RUNNING;
    }
}
