package com.oj.videostreamingserver.global.error;

import org.springframework.boot.autoconfigure.web.WebProperties;
import org.springframework.boot.autoconfigure.web.reactive.error.AbstractErrorWebExceptionHandler;
import org.springframework.boot.web.reactive.error.ErrorAttributes;
import org.springframework.context.ApplicationContext;
import org.springframework.http.codec.ServerCodecConfigurer;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.reactive.function.server.RequestPredicates;
import org.springframework.web.reactive.function.server.RouterFunction;
import org.springframework.web.reactive.function.server.RouterFunctions;
import org.springframework.web.reactive.function.server.ServerResponse;

@Component
public class GlobalExceptionHandler extends AbstractErrorWebExceptionHandler {



    public GlobalExceptionHandler(ErrorAttributes errorAttributes,WebProperties webProperties, ApplicationContext applicationContext) {

        super(errorAttributes, webProperties.getResources(), applicationContext);
        setMessageWriters(ServerCodecConfigurer.create().getWriters());
    }

    @Override
    protected RouterFunction<ServerResponse> getRoutingFunction(ErrorAttributes errorAttributes) {
        return RouterFunctions.route(RequestPredicates.all(), request -> {
            Throwable error = getError(request);
            return ErrorResponse.globalExceptionHandler(error);
        });
    }


}
