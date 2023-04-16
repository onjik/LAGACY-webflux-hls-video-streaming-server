package com.oj.videostreamingserver.global.error;

import com.oj.videostreamingserver.global.dto.ResponseDto;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.validation.BindingResult;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.reactive.function.server.ServerResponse;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.ArrayList;
import java.util.List;

/**
 * Error Response
 * use {@link ErrorResponse#of(ErrorCode, BindingResult)} method to create
 */
@Getter
@Slf4j
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


    public static Mono<ServerResponse> of(Exception e){
        log.error("unhandled exception : {}",e.getMessage());
        e.printStackTrace();
        return ServerResponse.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
    }

    public static Mono<ServerResponse> of(ErrorCode code){
        return ServerResponse.status(code.getStatus()).build();
    }


    public static Mono<ServerResponse> of(ErrorCode errorCode, Mono<FieldError> fieldError) {
        return ServerResponse.status(errorCode.getStatus())
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(fieldError);
    }
}
