package com.oj.videostreamingserver.domain.vod.router;

import com.oj.videostreamingserver.domain.vod.handler.VodEncodingHandler;
import com.oj.videostreamingserver.global.error.ErrorResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.server.RouterFunction;
import org.springframework.web.reactive.function.server.RouterFunctions;
import org.springframework.web.reactive.function.server.ServerResponse;

@Configuration
@RequiredArgsConstructor
public class VodRouter {

    private final VodEncodingHandler vodHandler;

    @Bean
    public RouterFunction<ServerResponse> routerExample() {
        return RouterFunctions.route()
                .POST("/media/vods/{videoId}",vodHandler::videoInitPost)
                .GET("/media/vods/{videoId}/encoding/status",vodHandler::broadCastEncodingStatus)
                .DELETE("/medias/vods/{videoId}",vodHandler::deleteVideo)
                .build();
    }
}
