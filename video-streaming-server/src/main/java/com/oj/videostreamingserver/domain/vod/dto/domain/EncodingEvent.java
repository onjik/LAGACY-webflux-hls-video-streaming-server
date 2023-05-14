package com.oj.videostreamingserver.domain.vod.dto.domain;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Sinks;

public class EncodingEvent<T> {
    public enum Status {
        READY,RUNNING,COMPLETE,ERROR
    };
    private final Sinks.Many<T> sink;
    private volatile Status status;

    //예외시 실행할 폴백
    private final Runnable failureHandler;
    private final Runnable completeHandler;


    public EncodingEvent(Sinks.Many<T> sink, Runnable failureHandler, Runnable completeHandler) {
        this.sink = sink;
        this.status = Status.READY;
        this.failureHandler = failureHandler;
        this.completeHandler = completeHandler;
    }

    public EncodingEvent(Sinks.Many<T> sink) {
        this.sink = sink;
        this.status = Status.READY;
        this.failureHandler = () -> {};
        this.completeHandler = () -> {};
    }

    public Flux<T> getFlux(){
        return sink.asFlux();
    }
    public Status getStatus() {
        return status;
    }

    public void reportRunning(){
        this.status = Status.RUNNING;
    }
    public void reportComplete(){
        this.status = Status.COMPLETE;
        this.sink.tryEmitComplete();
        completeHandler.run();
    }

    public void reportError(Throwable e){
        this.status = Status.ERROR;
        this.sink.tryEmitError(e);
        this.sink.tryEmitComplete();
        failureHandler.run();
    }

    public void reportNext(T data){
        sink.emitNext(data, Sinks.EmitFailureHandler.FAIL_FAST);
    }
}
