package com.oj.videostreamingserver.domain.vod.dto;

import reactor.core.publisher.Sinks;

public class EncodingEvent {
    public enum Status {READY,RUNNING, FINISHED, ERROR};
    private final Sinks.Many<String> sink;
    private volatile Status status;

    public EncodingEvent(Sinks.Many<String> sink) {
        this.sink = sink;
        this.status = Status.READY;
    }

    public Sinks.Many<String> getSink() {
        return sink;
    }

    public Status getStatus() {
        return status;
    }

    public synchronized void setStatus(Status status) {
        switch (status){
            case ERROR:
                sink.tryEmitError(new Exception("Encoding Error"));
                break;
            case FINISHED:
                sink.tryEmitComplete();
                break;
            case RUNNING:
                sink.tryEmitNext("Encoding Start");
                break;
        }
        this.status = status;
    }
}
