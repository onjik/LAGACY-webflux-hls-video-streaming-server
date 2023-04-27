package com.oj.videostreamingserver.domain.vod.router;

import com.oj.videostreamingserver.domain.vod.handler.VodPostHandler;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.WebFluxTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.DefaultDataBufferFactory;
import org.springframework.http.MediaType;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.server.ServerRequest;
import org.springframework.web.reactive.function.server.ServerResponse;
import reactor.core.publisher.Flux;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(SpringExtension.class)
@ContextConfiguration(classes = {VodRouter.class})
@WebFluxTest
class VodRouterTest {

    @Autowired
    WebTestClient webClient;

    @MockBean
    VodPostHandler handler;

//    @Nested
//    @DisplayName("POST /media")
//    class postMedia{
//
//        @Test
//        @DisplayName("정상적인 핸들링")
//        public void postMediaRoute(){
//            //given sample data
//            byte[] fileContent = new byte[] { 0x00, 0x01, 0x02, 0x03 };
//            DataBuffer dataBuffer = new DefaultDataBufferFactory().wrap(fileContent);
//            Flux<DataBuffer> data = Flux.just(dataBuffer);
//
//            //mocking
//            when(handler.postVideo(any(ServerRequest.class)))
//                    .thenReturn(ServerResponse.ok().build());
//
//            //when
//            webClient.post()
//                    .uri("/media")
//                    .contentType(MediaType.valueOf("video/mp4"))
//                    .body(BodyInserters.fromDataBuffers(data))
//                    .exchange()
//                    .expectStatus().isOk();
//
//            verify(handler).postVideo(any(ServerRequest.class));
//        }
//
//    }


}