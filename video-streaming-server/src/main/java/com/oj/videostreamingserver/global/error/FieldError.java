package com.oj.videostreamingserver.global.error;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.validation.BindingResult;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class FieldError {
    private String field;
    private String value;
    private String reason;

    private FieldError(String field, String value, String reason) {
        this.field = field;
        this.value = value;
        this.reason = reason;
    }

    public static Mono<FieldError> of(String field, String value, String reason) {
        return Mono.just(new FieldError(field,value,reason));
    }

}