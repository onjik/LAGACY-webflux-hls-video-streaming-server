package com.oj.videostreamingserver.global.error.exception;

public class BadRequestException extends BusinessException{
    String reason;

    public BadRequestException(String reason) {
        this.reason = reason;
    }
}
