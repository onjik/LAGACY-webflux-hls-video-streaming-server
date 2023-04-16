package com.oj.videostreamingserver.learning_test;

import com.oj.videostreamingserver.domain.vod.exception.InvalidTargetPathException;
import com.oj.videostreamingserver.global.error.ErrorCode;
import com.oj.videostreamingserver.global.error.ErrorResponse;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.web.reactive.function.server.ServerResponse;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

class ErrorHandlingTest {

    @Test
    void errorHandling(){
        Mono<Object> objectMono = Mono.error(new InvalidTargetPathException())
                .onErrorResume(InvalidTargetPathException.class, e -> ErrorResponse.of(ErrorCode.INVALID_TYPE_VALUE))
                .onErrorResume(Exception.class, e -> ErrorResponse.of(ErrorCode.INTERNAL_SERVER_ERROR));

        StepVerifier.create(objectMono)
                .expectSubscription()
                .expectNextMatches(response -> response instanceof ServerResponse && ((ServerResponse)response).statusCode().equals(HttpStatus.BAD_REQUEST) )
                .verifyComplete();

    }
}