package com.oj.videostreamingserver.domain.vod.router;

import com.oj.videostreamingserver.domain.vod.handler.EncodingHandler;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.server.RouterFunction;
import org.springframework.web.reactive.function.server.RouterFunctions;
import org.springframework.web.reactive.function.server.ServerResponse;

@Configuration
@RequiredArgsConstructor
public class VodRouter {

    private final EncodingHandler vodHandler;

    @Bean
    public RouterFunction<ServerResponse> routerExample() {
        return RouterFunctions.route()
                .POST("/media/vods/{videoId}",vodHandler::insertVideoDomain)
                .DELETE("/media/vods/{videoId}",vodHandler::deleteVideoDomain)
                .PATCH("/media/vods/{videoId}/thumbnail",vodHandler::updateThumbnail)
                .GET("/media/vods/{videoId}/encoding/statuses",vodHandler::broadCastEncodingStatus)
                .build();
    }
}
