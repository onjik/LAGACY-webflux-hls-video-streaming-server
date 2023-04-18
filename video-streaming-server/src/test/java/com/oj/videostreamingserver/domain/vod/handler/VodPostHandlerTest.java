package com.oj.videostreamingserver.domain.vod.handler;

import com.oj.videostreamingserver.domain.vod.domain.DraftVideo;
import com.oj.videostreamingserver.domain.vod.dto.OriginalVideoPostResponse;
import com.oj.videostreamingserver.domain.vod.repository.DraftVideoRepository;
import com.oj.videostreamingserver.domain.vod.service.FileService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.DefaultDataBufferFactory;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.client.MultipartBodyBuilder;
import org.springframework.http.codec.multipart.FilePart;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.springframework.transaction.reactive.TransactionalOperator;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.server.RouterFunction;
import org.springframework.web.reactive.function.server.RouterFunctions;
import org.springframework.web.reactive.function.server.ServerRequest;
import org.springframework.web.reactive.function.server.ServerResponse;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.lang.reflect.Method;
import java.nio.file.Paths;
import java.util.Arrays;

import static org.mockito.Mockito.*;

import static org.junit.jupiter.api.Assertions.*;

class VodPostHandlerTest {


    @Mock
    private TransactionalOperator transactionalOperator;

    @Mock
    private DraftVideoRepository draftVideoRepository;

    @Mock
    private FileService fileService;


    private VodPostHandler vodPostHandler;


    @BeforeEach
    private void initSpyVodPostHandler() throws NoSuchMethodException {
        transactionalOperator = mock(TransactionalOperator.class);
        fileService = mock(FileService.class);
        draftVideoRepository = mock(DraftVideoRepository.class);
        vodPostHandler = new VodPostHandler(transactionalOperator,fileService,draftVideoRepository);
        ReflectionTestUtils.setField(vodPostHandler,"MEDIA_VOLUME_ROOT","app/media",String.class);
    }

    //왜캐 복잡한겨 ㅠㅠ
    @Test
    @DisplayName("원본 비디오 포스팅 API 테스트 - 요청 성공")
    public void postVideoTest() throws IOException {
        // given
        DraftVideo draftVideo = new DraftVideo("/test/file/path.mp4",1L);
        ReflectionTestUtils.setField(draftVideo,"id",1L);

        // multipart body
        MultipartBodyBuilder builder = new MultipartBodyBuilder();
        builder.part("video", new FileSystemResource(File.createTempFile("test",".mp4")));
        MultiValueMap<String, HttpEntity<?>> build = builder.build();



        //mock
        when(fileService.saveVideoToDraft(any(FilePart.class))).thenReturn(Mono.just(new File("/test/fake/video.mp4")));
        when(draftVideoRepository.save(any(DraftVideo.class))).thenReturn(Mono.just(draftVideo));
        when(transactionalOperator.transactional(any(Mono.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        WebTestClient client = WebTestClient.bindToRouterFunction(RouterFunctions.route()
                .POST("/media", request -> vodPostHandler.postVideo(request)).build()
        ).build();



        //when
        client.post()
                .uri("/media")
                .contentType(MediaType.MULTIPART_FORM_DATA)
                .body(BodyInserters.fromMultipartData(build))
                .exchange()
        //then
                .expectStatus().isOk()
                .expectHeader().contentType(MediaType.APPLICATION_JSON)
                .expectBody(OriginalVideoPostResponse.class)
                .isEqualTo(new OriginalVideoPostResponse(draftVideo.getId()));
    }



}