package com.oj.videostreamingserver.domain.vod.handler;

import com.oj.videostreamingserver.domain.vod.component.EncodingChannel;
import com.oj.videostreamingserver.domain.vod.component.PathManager;
import com.oj.videostreamingserver.domain.vod.service.EncodingService;
import com.oj.videostreamingserver.domain.vod.service.FileService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.io.DefaultResourceLoader;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpEntity;
import org.springframework.http.client.MultipartBodyBuilder;
import org.springframework.http.codec.multipart.FilePart;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.springframework.util.MultiValueMap;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.server.RouterFunctions;
import reactor.core.publisher.Mono;

import java.io.File;
import java.lang.reflect.Field;
import java.nio.file.Path;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class VodEncodingHandlerTest {

    VodEncodingHandler vodEncodingHandler;

    EncodingService encodingService;
    FileService fileService;

    EncodingChannel encodingChannel;

    @BeforeEach
    void setUp() {
        this.encodingService = mock(EncodingService.class);
        this.fileService = mock(FileService.class);
        this.encodingChannel = mock(EncodingChannel.class);
        this.vodEncodingHandler = new VodEncodingHandler(encodingService,fileService,encodingChannel);
    }

    @Nested
    @DisplayName("videoInitPost 메서드")
    class videoInitPost{
        WebTestClient webTestClient;
        @BeforeEach
        void setup() throws NoSuchFieldException, IllegalAccessException {
            //given
            this.webTestClient = WebTestClient.bindToRouterFunction(RouterFunctions
                    .route()
                    .POST("/media", vodEncodingHandler::videoInitPost)
                    .build()).build();

            //mock
            String tempDirectoryPath = System.getProperty("java.io.tmpdir");
            Path testRootPath = Path.of(tempDirectoryPath).resolve("forTest");
            Field mediaRootPath = PathManager.class.getDeclaredField("mediaRootPath");
            mediaRootPath.setAccessible(true);
            mediaRootPath.set(null, testRootPath);
        }

        @Test
        @DisplayName("필수 항목이 없을 때")
        void videoInitPost_WhenNoVideoFile() {
            //mock
            webTestClient.post()
                    .uri("/media")
                    .exchange()
                    .expectStatus()
                    .isBadRequest();

        }

        @Test
        @DisplayName("썸네일이 포함되었지만, 파일이 아닐 때")
        void videoInitPost_WhenThumbnailIsNotFile() {
            MultipartBodyBuilder builder = new MultipartBodyBuilder();
            builder.part("thumbnail","notFile");
            builder.part("videoId", UUID.randomUUID());
            MultiValueMap<String, HttpEntity<?>> multiValueMap = builder.build();
            //비디오 파일 삽입
            Resource videoFile = new DefaultResourceLoader().getResource("classpath:sample.mp4");
            builder.part("video", videoFile);
            //mock
            webTestClient.post()
                    .uri("/media")
                    .body(BodyInserters.fromMultipartData(multiValueMap))
                    .exchange()
                    .expectStatus()
                    .isBadRequest();
        }

        @Test
        @DisplayName("정상적인 처리의 경우 - 썸네일을 제공할 때")
        void videoInitPost_WhenSuccess() {
            MultipartBodyBuilder builder = new MultipartBodyBuilder();
            builder.part("videoId", UUID.randomUUID().toString());
            //비디오 파일 삽입
            Resource videoFile = new DefaultResourceLoader().getResource("classpath:sample.mp4");
            builder.part("video", videoFile);
            Resource thumbnailFile = new DefaultResourceLoader().getResource("classpath:sample.jpg");
            builder.part("thumbnail", thumbnailFile);
            MultiValueMap<String, HttpEntity<?>> valueMap = builder.build();

            //mock
            when(fileService.saveFilePart(any(FilePart.class),any(Path.class))).thenReturn(Mono.<Void>empty());
            when(encodingService.encodeThumbnail(any(UUID.class),any(File.class))).thenReturn(Mono.<Void>empty());
            when(encodingService.encodeVideo(any(UUID.class),any(Path.class),anyList())).thenReturn(Mono.<Void>empty());

            webTestClient.post()
                    .uri("/media")
                    .body(BodyInserters.fromMultipartData(valueMap))
                    .exchange()
                    .expectStatus()
                    .isOk();
        }
    }

}