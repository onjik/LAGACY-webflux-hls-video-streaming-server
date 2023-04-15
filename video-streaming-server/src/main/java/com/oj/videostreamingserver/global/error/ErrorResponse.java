package com.oj.videostreamingserver.global.error;

import com.oj.videostreamingserver.global.dto.ResponseDto;
import lombok.Getter;
import org.springframework.validation.BindingResult;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import java.util.ArrayList;
import java.util.List;

/**
 * Error Response
 * use {@link ErrorResponse#of(ErrorCode, BindingResult)} method to create
 */
@Getter
public class ErrorResponse extends ResponseDto {
    private String message;
    private String status;
    private List<FieldError> errors;
    private String code;

    private ErrorResponse(ErrorCode code, List<FieldError> errors){
        super(false);
        this.message = code.getMessage();
        this.status = code.getStatus().toString();
        this.errors = errors;
        this.code = code.getCode();
    }

    private ErrorResponse(ErrorCode code){
        super(false);
        this.message = code.getMessage();
        this.status = code.getStatus().toString();
        this.errors = new ArrayList<>(); //if errors is null, response empty list instead of null
        this.code = code.getCode();
    }

    public static ErrorResponse of(ErrorCode code){
        return new ErrorResponse(code);
    }

    public static ErrorResponse of(ErrorCode code, BindingResult bindingResult){
        return new ErrorResponse(code, FieldError.of(bindingResult));
    }

    public static ErrorResponse of(MethodArgumentTypeMismatchException e){
        String value = e.getValue() == null ? "" : e.getValue().toString();
        List<FieldError> errors = FieldError.of(e.getName(), value, e.getErrorCode());
        return new ErrorResponse(ErrorCode.INVALID_TYPE_VALUE,errors);
    }

    public static ErrorResponse of(ErrorCode errorCode, List<FieldError> fieldErrors) {
        return new ErrorResponse(errorCode, fieldErrors);
    }
}
