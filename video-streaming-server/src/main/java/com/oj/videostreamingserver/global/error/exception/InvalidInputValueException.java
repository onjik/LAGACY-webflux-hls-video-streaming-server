package com.oj.videostreamingserver.global.error.exception;

import com.oj.videostreamingserver.global.error.ErrorCode;
import com.oj.videostreamingserver.global.error.FieldError;

import java.util.Collections;
import java.util.List;

/**
 * 클라이언트가 넘겨준 값들이 잘못 되었을 때 던져지는 예외
 * Http Status 400 Bad Request 로 응답을 함
 */
public class InvalidInputValueException extends BusinessException{
    public InvalidInputValueException(List<FieldError> fieldErrors) {
        super(ErrorCode.INVALID_INPUT_VALUE, fieldErrors);
    }
    public InvalidInputValueException(String field, String value, String reason) {
        super(ErrorCode.INVALID_INPUT_VALUE, FieldError.of(field, value, reason));
    }


    public InvalidInputValueException() {
        super(ErrorCode.INVALID_INPUT_VALUE, Collections.emptyList());
    }

}
