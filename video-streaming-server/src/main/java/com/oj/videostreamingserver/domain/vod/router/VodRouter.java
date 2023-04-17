package com.oj.videostreamingserver.domain.vod.router;

import com.oj.videostreamingserver.domain.vod.handler.VodPostHandler;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.server.RouterFunction;
import org.springframework.web.reactive.function.server.RouterFunctions;
import org.springframework.web.reactive.function.server.ServerResponse;

@Configuration
@RequiredArgsConstructor
public class VodRouter {

    private final VodPostHandler vodHandler;

    @Bean
    public RouterFunction<ServerResponse> routerExample() {
        return RouterFunctions.route()
                .POST("/media", vodHandler::postVideo)
                .build();
    }
}
