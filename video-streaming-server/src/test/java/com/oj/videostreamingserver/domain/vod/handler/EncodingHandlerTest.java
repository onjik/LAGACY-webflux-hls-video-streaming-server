package com.oj.videostreamingserver.domain.vod.handler;

import com.oj.videostreamingserver.domain.vod.component.EncodingChannel;
import com.oj.videostreamingserver.domain.vod.component.PathManager;
import com.oj.videostreamingserver.domain.vod.domain.VideoEntry;
import com.oj.videostreamingserver.domain.vod.domain.VideoMediaEntry;
import com.oj.videostreamingserver.domain.vod.router.VodRouter;
import com.oj.videostreamingserver.domain.vod.service.EncodingService;
import com.oj.videostreamingserver.domain.vod.service.FileService;
import com.oj.videostreamingserver.global.error.exception.InvalidInputValueException;
import net.bramp.ffmpeg.probe.FFmpegProbeResult;
import net.bramp.ffmpeg.probe.FFmpegStream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.io.DefaultResourceLoader;
import org.springframework.core.io.Resource;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.DataBufferUtils;
import org.springframework.core.io.buffer.DefaultDataBufferFactory;
import org.springframework.data.r2dbc.core.R2dbcEntityTemplate;
import org.springframework.data.relational.core.query.Query;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.codec.multipart.FilePart;
import org.springframework.http.codec.multipart.FormFieldPart;
import org.springframework.http.codec.multipart.Part;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.mock.web.reactive.function.server.MockServerRequest;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.springframework.transaction.reactive.TransactionalOperator;
import org.springframework.util.MimeType;
import org.springframework.util.MultiValueMap;
import org.springframework.util.MultiValueMapAdapter;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Field;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class EncodingHandlerTest {

    EncodingHandler encodingHandler;

    EncodingService encodingService;
    FileService fileService;
    R2dbcEntityTemplate template;

    EncodingChannel encodingChannel;
    TransactionalOperator transactionalOperator;

    @BeforeEach
    void setUp() {
        this.encodingService = mock(EncodingService.class);
        this.fileService = mock(FileService.class);
        this.encodingChannel = mock(EncodingChannel.class);
        this.template = mock(R2dbcEntityTemplate.class);
        this.transactionalOperator = mock(TransactionalOperator.class);
        this.encodingHandler = new EncodingHandler(encodingService,fileService,encodingChannel,transactionalOperator,template);
    }

    @Nested
    @DisplayName("videoInitPost 메서드")
    class videoInitPost{
        WebTestClient webTestClient;
        @BeforeEach
        void setup() throws NoSuchFieldException, IllegalAccessException {
            //given
            this.webTestClient = WebTestClient.bindToRouterFunction(
                    new VodRouter(encodingHandler)
                            .routerExample())
                    .build();

            //mock
            String tempDirectoryPath = System.getProperty("java.io.tmpdir");
            Path testRootPath = Path.of(tempDirectoryPath).resolve("forTest");
            Field mediaRootPath = PathManager.class.getDeclaredField("mediaRootPath");
            mediaRootPath.setAccessible(true);
            mediaRootPath.set(null, testRootPath);

        }

        @Test
        @DisplayName("정상적인 처리의 경우 - 썸네일을 제공할 때")
        void videoInitPost_WhenSuccess() throws IOException {
            Map<String,List<FilePart>> map = new HashMap<>();

            Resource videoFile = new DefaultResourceLoader().getResource("classpath:sample.mp4");
            List<FilePart> videoPart = List.of(createFilePart(videoFile,MediaType.valueOf("video/mp4")));
            map.put("video",videoPart);

            Resource thumbnailFile = new DefaultResourceLoader().getResource("classpath:sample.jpg");
            List<FilePart> thumbnailPart = List.of(createFilePart(thumbnailFile,MediaType.valueOf("image/jpeg")));
            map.put("thumbnail",thumbnailPart);


            //mock
            when(fileService.saveFilePart(any(FilePart.class),any(Path.class))).thenReturn(Mono.<Void>empty());
            when(encodingService.encodeThumbnail(any(UUID.class),any(File.class))).thenReturn(Mono.<Void>empty());
            FFmpegProbeResult mockResult = mock(FFmpegProbeResult.class);
            List mockList = mock(List.class);
            FFmpegStream mockStream = mock(FFmpegStream.class);

            when(mockResult.getStreams()).thenReturn(mockList);
            when(mockList.get(0)).thenReturn(mockStream);
            ReflectionTestUtils.setField(mockStream,"duration",1000.0);


            when(encodingService.encodeVideo(any(UUID.class),any(Path.class),anyList())).thenReturn(Mono.just(mockResult));
            when(template.exists(any(Query.class),eq(VideoEntry.class))).thenReturn(Mono.just(true));
            when(template.exists(any(Query.class),eq(VideoMediaEntry.class))).thenReturn(Mono.just(false));



            MultiValueMap<String, FilePart> valueMap = new MultiValueMapAdapter<>(map);

            MockServerRequest request = MockServerRequest.builder()
                    .pathVariable("videoId",UUID.randomUUID().toString())
                    .body(Mono.just(valueMap));
            StepVerifier.create(encodingHandler.insertVideoDomain(request))
                    .expectNextCount(1)
                    .verifyComplete();
        }

        @Test
        @DisplayName("필수 항목이 없을 때")
        void videoInitPost_WhenNoVideoFile() throws IOException {
            Map<String,List<FilePart>> map = new HashMap<>();

            MultiValueMap<String, FilePart> valueMap = new MultiValueMapAdapter<>(map);

            MockServerRequest request = MockServerRequest.builder()
                    .pathVariable("videoId",UUID.randomUUID().toString())
                    .body(Mono.just(valueMap));
            StepVerifier.create(encodingHandler.insertVideoDomain(request))
                    .expectError(InvalidInputValueException.class)
                    .verify();

        }

        @Test
        @DisplayName("썸네일이 포함되었지만, 파일이 아닐 때")
        void videoInitPost_WhenThumbnailIsNotFile() throws IOException {
            Map<String,List<Part>> map = new HashMap<>();

            Resource videoFile = new DefaultResourceLoader().getResource("classpath:sample.mp4");
            List<Part> videoPart = List.of(createFilePart(videoFile,MediaType.valueOf("video/mp4")));
            map.put("video",videoPart);

            Resource thumbnailFile = new DefaultResourceLoader().getResource("classpath:sample.jpg");
            List<Part> thumbnailPart = List.of(createFormFieldPart("thumbnail String"));
            map.put("thumbnail",thumbnailPart);


            MultiValueMap<String, Part> valueMap = new MultiValueMapAdapter<>(map);

            MockServerRequest request = MockServerRequest.builder()
                    .pathVariable("videoId",UUID.randomUUID().toString())
                    .body(Mono.just(valueMap));
            StepVerifier.create(encodingHandler.insertVideoDomain(request))
                    .expectError(InvalidInputValueException.class)
                    .verify();

        }

        private FormFieldPart createFormFieldPart(String content){
            return new FormFieldPart() {
                @Override
                public String value() {
                    return content;
                }

                @Override
                public String name() {
                    return content;
                }

                @Override
                public HttpHeaders headers() {
                    return new HttpHeaders();
                }

                @Override
                public Flux<DataBuffer> content() {
                    return Flux.empty();
                }
            };
        }

        private FilePart createFilePart(Resource source, MediaType mediaType) throws IOException {
            MockMultipartFile resource = new MockMultipartFile("video", source.getInputStream());
            return new FilePart() {
                @Override
                public String filename() {
                    return resource.getOriginalFilename();
                }

                @Override
                public Mono<Void> transferTo(Path dest) {
                    try {
                        resource.transferTo(dest);
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                    return Mono.empty();
                }

                @Override
                public String name() {
                    return resource.getName();
                }

                @Override
                public HttpHeaders headers() {
                    HttpHeaders httpHeaders = new HttpHeaders();
                    httpHeaders.setContentType(mediaType);
                    return httpHeaders;
                }

                @Override
                public Flux<DataBuffer> content() {
                    return DataBufferUtils.read(resource.getResource(),new DefaultDataBufferFactory(),Integer.MAX_VALUE);
                }
            };
        }

    }

}