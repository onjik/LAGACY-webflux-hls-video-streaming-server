package com.oj.videostreamingserver.domain.vod.handler;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.mockito.Mock;
import org.springframework.data.r2dbc.core.R2dbcEntityTemplate;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.transaction.reactive.TransactionalOperator;

import static org.mockito.Mockito.*;

@DisplayName("POST /media handler test")
class VodPostHandlerTest {


    @Mock
    private TransactionalOperator transactionalOperator;

    @Mock
    private R2dbcEntityTemplate r2dbcEntityTemplate;

    @Mock
    private FileService fileService;


    private VodPostHandler vodPostHandler;


    @BeforeEach
    private void initSpyVodPostHandler() {
        transactionalOperator = spy(TransactionalOperator.class);
        fileService = mock(FileService.class);
        r2dbcEntityTemplate = mock(R2dbcEntityTemplate.class);
//        vodPostHandler = spy(new VodPostHandler(transactionalOperator,fileService,r2dbcEntityTemplate));
        ReflectionTestUtils.setField(vodPostHandler,"MEDIA_VOLUME_ROOT","app/media",String.class);
    }

    //왜캐 복잡한겨 ㅠㅠ
//    @Test
//    @DisplayName("정상적인 요청")
//    public void postVideoTest() throws IOException {
//        // given
//        DraftVideo draftVideo = new DraftVideo("/test/file/path.mp4",1L);
//        ReflectionTestUtils.setField(draftVideo,"id",1L);
//
//        // multipart body
//        MultipartBodyBuilder builder = new MultipartBodyBuilder();
//        builder.part("video", new FileSystemResource(File.createTempFile("test",".mp4")));
//        MultiValueMap<String, HttpEntity<?>> build = builder.build();
//
//
//
//        //mock
//        when(fileService.saveVideoToDraft(any(FilePart.class))).thenReturn(Mono.just(new File("/test/fake/video.mp4")));
//        when(draftVideoRepository.save(any(DraftVideo.class))).thenReturn(Mono.just(draftVideo));
//        when(transactionalOperator.transactional(any(Mono.class)))
//                .thenAnswer(invocation -> invocation.getArgument(0));
//        WebTestClient client = WebTestClient.bindToRouterFunction(RouterFunctions.route()
//                .POST("/media", request -> vodPostHandler.postVideo(request)).build()
//        ).build();
//
//
//
//        //when
//        client.post()
//                .uri("/media")
//                .contentType(MediaType.MULTIPART_FORM_DATA)
//                .body(BodyInserters.fromMultipartData(build))
//                .exchange()
//        //then
//                .expectStatus().isOk()
//                .expectHeader().contentType(MediaType.APPLICATION_JSON)
//                .expectBody(OriginalVideoPostResponse.class)
//                .isEqualTo(new OriginalVideoPostResponse(draftVideo.getId()));
//    }
//    @Test
//    @DisplayName("multipart body 없이 요청 -> content type 헤더에서 걸러짐")
//    public void withNoContentType() throws IOException {
//        // given
//        DraftVideo draftVideo = new DraftVideo("/test/file/path.mp4",1L);
//        ReflectionTestUtils.setField(draftVideo,"id",1L);
//
//        // multipart body
//        MultipartBodyBuilder builder = new MultipartBodyBuilder();
//        builder.part("video", new FileSystemResource(File.createTempFile("test",".mp4")));
//        MultiValueMap<String, HttpEntity<?>> build = builder.build();
//
//
//
//        //mock
//        when(fileService.saveVideoToDraft(any(FilePart.class))).thenReturn(Mono.just(new File("/test/fake/video.mp4")));
//        when(draftVideoRepository.save(any(DraftVideo.class))).thenReturn(Mono.just(draftVideo));
//        when(transactionalOperator.transactional(any(Mono.class)))
//                .thenAnswer(invocation -> invocation.getArgument(0));
//        WebTestClient client = WebTestClient.bindToRouterFunction(RouterFunctions.route()
//                .POST("/media", request -> vodPostHandler.postVideo(request)).build()
//        ).build();
//
//
//
//        //when
//        client.post()
//                .uri("/media")
//                .exchange()
//                //then
//                .expectStatus().isBadRequest()
//                .expectHeader().contentType(MediaType.APPLICATION_JSON)
//                .expectBody(ErrorResponse.class);
//    }
//
//    @Test
//    @DisplayName("multipart 안에 video 필드가 없는 경우 -> bad request")
//    public void invalidMultipartField() throws IOException {
//        // given
//        DraftVideo draftVideo = new DraftVideo("/test/file/path.mp4",1L);
//        ReflectionTestUtils.setField(draftVideo,"id",1L);
//
//        // multipart body
//        MultipartBodyBuilder builder = new MultipartBodyBuilder();
//        builder.part("notVideo", new FileSystemResource(File.createTempFile("test",".mp4")));
//        MultiValueMap<String, HttpEntity<?>> build = builder.build();
//
//
//
//        //mock
//        when(fileService.saveVideoToDraft(any(FilePart.class))).thenReturn(Mono.just(new File("/test/fake/video.mp4")));
//        when(draftVideoRepository.save(any(DraftVideo.class))).thenReturn(Mono.just(draftVideo));
//        when(transactionalOperator.transactional(any(Mono.class)))
//                .thenAnswer(invocation -> invocation.getArgument(0));
//        WebTestClient client = WebTestClient.bindToRouterFunction(RouterFunctions.route()
//                .POST("/media", request -> vodPostHandler.postVideo(request)).build()
//        ).build();
//
//
//
//        //when
//        client.post()
//                .uri("/media")
//                .contentType(MediaType.MULTIPART_FORM_DATA)
//                .body(BodyInserters.fromMultipartData(build))
//                .exchange()
//                //then
//                .expectStatus().isBadRequest()
//                .expectHeader().contentType(MediaType.APPLICATION_JSON)
//                .expectBody(ErrorResponse.class);
//    }
//
//    @Test
//    @DisplayName("video 필드가 여러개인 경우 -> Bad Request")
//    public void multipleVideo() throws IOException {
//        // given
//        DraftVideo draftVideo = new DraftVideo("/test/file/path.mp4",1L);
//        ReflectionTestUtils.setField(draftVideo,"id",1L);
//
//        // multipart body
//        MultipartBodyBuilder builder = new MultipartBodyBuilder();
//        builder.part("video", new FileSystemResource(File.createTempFile("test1",".mp4")));
//        builder.part("video", new FileSystemResource(File.createTempFile("test2",".mp4")));
//        MultiValueMap<String, HttpEntity<?>> build = builder.build();
//
//
//
//        //mock
//        when(fileService.saveVideoToDraft(any(FilePart.class))).thenReturn(Mono.just(new File("/test/fake/video.mp4")));
//        when(draftVideoRepository.save(any(DraftVideo.class))).thenReturn(Mono.just(draftVideo));
//        when(transactionalOperator.transactional(any(Mono.class)))
//                .thenAnswer(invocation -> invocation.getArgument(0));
//        WebTestClient client = WebTestClient.bindToRouterFunction(RouterFunctions.route()
//                .POST("/media", request -> vodPostHandler.postVideo(request)).build()
//        ).build();
//
//
//
//        //when
//        client.post()
//                .uri("/media")
//                .contentType(MediaType.MULTIPART_FORM_DATA)
//                .body(BodyInserters.fromMultipartData(build))
//                .exchange()
//                //then
//                .expectStatus().isBadRequest()
//                .expectHeader().contentType(MediaType.APPLICATION_JSON)
//                .expectBody(ErrorResponse.class);
//    }
//
//
//    @Test
//    @DisplayName("데이터 베이스 오류시 파일 삭제")
//    public void fileTransaction() throws IOException {
//        // given
//        File transactionTarget = File.createTempFile("test", ".mp4");
//        FilePart filePart = new FilePart() {
//            @Override
//            public String filename() {
//                return transactionTarget.getName();
//            }
//
//            @Override
//            public Mono<Void> transferTo(Path dest) {
//                return null;
//            }
//
//            @Override
//            public String name() {
//                return null;
//            }
//
//            @Override
//            public HttpHeaders headers() {
//                return null;
//            }
//
//            @Override
//            public Flux<DataBuffer> content() {
//                return null;
//            }
//        };
//        doReturn(Mono.just(filePart)).when(vodPostHandler).checkRequestValidation(any(ServerRequest.class));
//        when(fileService.saveVideoToDraft(any(FilePart.class)))
//                .thenReturn(Mono.just(transactionTarget));
//        when(draftVideoRepository.save(any(DraftVideo.class)))
//                .thenThrow(new RuntimeException());
//        when(transactionalOperator.transactional(any(Mono.class)))
//                .thenAnswer(invocation -> invocation.getArgument(0));
//
//        // when
//        Mono<ServerResponse> responseMono = vodPostHandler.postVideo(MockServerRequest.builder().build());
//
//
//
//        // then
//        StepVerifier.create(responseMono)
//                .expectNextMatches(serverResponse -> serverResponse.statusCode().isError() && !transactionTarget.exists())
//                .verifyComplete();
//
//    }



}