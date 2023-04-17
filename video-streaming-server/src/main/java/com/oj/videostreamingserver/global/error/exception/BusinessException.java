package com.oj.videostreamingserver.global.error.exception;

import com.oj.videostreamingserver.global.error.ErrorCode;
import com.oj.videostreamingserver.global.error.FieldError;
import lombok.Getter;

import java.util.List;

@Getter
public class BusinessException extends RuntimeException{

    private final ErrorCode errorCode;
    private final List<FieldError> fieldErrors;

    public BusinessException(ErrorCode errorCode, List<FieldError> fieldErrors) {
        this.errorCode = errorCode;
        this.fieldErrors = fieldErrors;
    }
}
