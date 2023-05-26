package com.oj.videostreamingserver.domain.vod.service;

import com.oj.videostreamingserver.domain.vod.component.EncodingChannel;
import com.oj.videostreamingserver.domain.vod.util.PathManager;
import com.oj.videostreamingserver.domain.vod.domain.VideoMediaEntry;
import com.oj.videostreamingserver.domain.vod.dto.domain.EncodingEvent;
import net.bramp.ffmpeg.FFprobe;
import net.bramp.ffmpeg.probe.FFmpegProbeResult;
import org.apache.commons.io.FileUtils;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.io.DefaultResourceLoader;
import org.springframework.core.io.Resource;
import org.springframework.data.r2dbc.core.R2dbcEntityTemplate;
import org.springframework.transaction.reactive.TransactionalOperator;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Field;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.*;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.mockito.Mockito.mock;


@ExtendWith(MockitoExtension.class)
class EncodingServiceTest {
    //test target
    EncodingService encodingService;
    FFprobe ffprobe;
    ExecuteService executeService;
    R2dbcEntityTemplate r2dbcEntityTemplate;
    TransactionalOperator transactionalOperator;
    EncodingChannel encodingChannel;


    @BeforeEach
    void setUp() throws IOException {
        //mock
        this.r2dbcEntityTemplate = mock(R2dbcEntityTemplate.class);
        this.transactionalOperator = mock(TransactionalOperator.class);
        this.encodingChannel = new EncodingChannel();

        this.ffprobe = new FFprobe("ffprobe");
        executeService = new ExecuteService(this.encodingChannel);

        this.encodingService = new EncodingService(ffprobe, executeService, r2dbcEntityTemplate, transactionalOperator);
    }

    @Nested
    @DisplayName("buildHlsEncodeCommand 메서드")
    class buildHlsEncodeCommand{
        @Test
        @DisplayName("오디오가 있을 때 제대로 커맨드가 만들어 지는지 테스트")
        void buildCommandWithAudio(){

            Path input = Path.of("/Users/kim-onji/Documents/GitHub/webflux-hls-video-streaming-server/video-streaming-server/app/test/vod/787cd051-aea5-48c5-81b9-c4df7e9ed2db/sample.mp4");
            Path seg = Path.of("/Users/kim-onji/Documents/GitHub/webflux-hls-video-streaming-server/video-streaming-server/app/test/vod/787cd051-aea5-48c5-81b9-c4df7e9ed2db/%v/file_%03d.ts");
            Path output = Path.of("/Users/kim-onji/Documents/GitHub/webflux-hls-video-streaming-server/video-streaming-server/app/test/vod/787cd051-aea5-48c5-81b9-c4df7e9ed2db/%v/index.m3u8");
            String expected = "ffmpeg -i " + input.toString() +
                    " -preset ultrafast -threads 1 " +
                    "-c:v libx264 -crf 22 -c:a aac -ar 48000 " +
                    "-f hls -hls_time 10 -hls_playlist_type vod -hls_list_size 0 -hls_flags independent_segments " +
                    "-progress - -nostats -v quiet " +
                    "-map 0:v:0 -map 0:a:0 -filter:v:0 scale=-2:360 " +
                    "-map 0:v:0 -map 0:a:0 -filter:v:1 scale=-2:720 " +
                    "-map 0:v:0 -map 0:a:0 -filter:v:2 scale=-2:1080 " +
                    "-var_stream_map v:0,a:0,name:360 v:1,a:1,name:720 v:2,a:2,name:1080 " +
                    "-master_pl_name master.m3u8 " +
                    "-hls_segment_filename " + seg + " " + output;

            String[] command = encodingService.buildHlsEncodeCommand(List.of(360, 720, 1080), input.toFile(), input.getParent(), true);
            String collect = Arrays.stream(command).collect(Collectors.joining(" "));
            assertEquals(expected, collect);
        }


        @Test
        @DisplayName("오디오가 없을때 제대로 만들어지는지 테스트")
        void buildCommandWithoutAudio() {
            Path input = Path.of("/Users/kim-onji/Documents/GitHub/webflux-hls-video-streaming-server/video-streaming-server/app/test/vod/787cd051-aea5-48c5-81b9-c4df7e9ed2db/sample.mp4");
            Path seg = Path.of("/Users/kim-onji/Documents/GitHub/webflux-hls-video-streaming-server/video-streaming-server/app/test/vod/787cd051-aea5-48c5-81b9-c4df7e9ed2db/%v/file_%03d.ts");
            Path output = Path.of("/Users/kim-onji/Documents/GitHub/webflux-hls-video-streaming-server/video-streaming-server/app/test/vod/787cd051-aea5-48c5-81b9-c4df7e9ed2db/%v/index.m3u8");
            String expected = "ffmpeg -i " + input +
                    " -preset ultrafast -threads 1 " +
                    "-c:v libx264 -crf 22 -c:a aac -ar 48000 " +
                    "-f hls -hls_time 10 -hls_playlist_type vod -hls_list_size 0 -hls_flags independent_segments " +
                    "-progress - -nostats -v quiet " +
                    "-map 0:v:0 -filter:v:0 scale=-2:360 " +
                    "-map 0:v:0 -filter:v:1 scale=-2:720 " +
                    "-map 0:v:0 -filter:v:2 scale=-2:1080 " +
                    "-var_stream_map v:0,name:360 v:1,name:720 v:2,name:1080 " +
                    "-master_pl_name master.m3u8 " +
                    "-hls_segment_filename " + seg + " " + output;

            String[] command = encodingService.buildHlsEncodeCommand(List.of(360, 720, 1080), input.toFile(), input.getParent(), false);
            String collect = Arrays.stream(command).collect(Collectors.joining(" "));
            assertEquals(expected, collect);
        }
    }

    @Nested
    @DisplayName("encodeVideo 메서드")
    class fileRelatedTest{

        UUID testVideoId; //테스트용 비디오 아이디
        Path testRootPath; //시스템의 임시 디렉토리
        Path videoDomainPath; //비디오 파일이 저장될 디렉토리 (videoId가 최종 폴더 이름)
        File copiedFile; //복사된 파일

        @BeforeEach
        void setUp() throws IOException, NoSuchFieldException, IllegalAccessException {
            //given
            testVideoId = UUID.fromString("787cd051-aea5-48c5-81b9-c4df7e9ed2db");

            //테스트를 위해 빌드된 파일을 불러옴
            String tempDirectoryPath = System.getProperty("java.io.tmpdir");
            this.testRootPath = Path.of(tempDirectoryPath).resolve("forTest");
            //mock
            Field mediaRootPath = PathManager.class.getDeclaredField("mediaRootPath");
            mediaRootPath.setAccessible(true);
            mediaRootPath.set(null, testRootPath);
            this.videoDomainPath = PathManager.VodPath.rootOf(testVideoId);



            Resource resource = new DefaultResourceLoader().getResource("classpath:sample.mp4");
            videoDomainPath.toFile().mkdirs();
            this.copiedFile = videoDomainPath.resolve("sample.mp4").toFile();

            //resource를 tempFile로 복사
            Files.copy(resource.getInputStream(), copiedFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
            System.out.println(copiedFile.toPath());


        }

        @AfterEach
        void tearDown() throws IOException {
            //테스트가 끝나면 임시 파일을 삭제
            FileUtils.deleteDirectory(testRootPath.toFile());
        }



        @Test
        @DisplayName("비디오 인코딩 테스트")
        void videoEncode() throws NoSuchFieldException, IllegalAccessException, IOException {

            //mock
            when(transactionalOperator.transactional(any(Mono.class))).thenAnswer(invocation -> invocation.getArgument(0));
            when(r2dbcEntityTemplate.insert(any(VideoMediaEntry.class))).thenReturn(Mono.empty());


            //given
            List<Integer> resolutionCandidates = List.of(360, 480, 720, 1080);
            String broadKey = encodingChannel.keyResolver(testVideoId, EncodingChannel.Type.VIDEO);


            Path ogVideoPath = copiedFile.toPath();


            //when
            Mono<FFmpegProbeResult> fFmpegProbeResultMono = encodingService.encodeVideo(testVideoId, ogVideoPath, resolutionCandidates);

            //then
            //정상적으로 끝났는지 검증
            StepVerifier.create(fFmpegProbeResultMono)
                    .expectNextCount(1)
                    .verifyComplete();

            //중계 채널에서 정상적으로 메시지를 날리는지 검증
            EncodingEvent<String> stringEncodingEvent = encodingChannel.getEncodingEvent(broadKey).get();
            assertNotNull(stringEncodingEvent);
            List<String> messages = stringEncodingEvent.getFlux()
                    .doOnNext(System.out::println)
                    .doOnError(s -> fail())
                    .buffer().blockLast();
            assertFalse(messages.isEmpty());

            //파일들이 잘 저장되었는지 확인
            List<Path> tempPathFileTree = Files.walk(ogVideoPath.getParent()).collect(Collectors.toList());
            System.out.println("<테스트 디버깅 용 : 임시 경로의 파일들>");
            tempPathFileTree.stream().forEach(System.out::println);
            List<Path> expect = new ArrayList<>();
            expect.add(videoDomainPath);
            expect.add(videoDomainPath.resolve("master.m3u8"));
            expect.add(videoDomainPath.resolve("360"));
            expect.add(videoDomainPath.resolve("360").resolve("index.m3u8"));
            expect.add(videoDomainPath.resolve("360").resolve("file_000.ts"));
            expect.add(videoDomainPath.resolve("720").resolve("index.m3u8"));
            System.out.println("-----------------------");
            for (Path path : expect){
                assertTrue(tempPathFileTree.contains(path), path.toString() + "가 없습니다.");
            }
        }

        @Test
        @DisplayName("썸네일 인코딩 테스트")
        void encodeThumbnail(){
            //given
            String broadKey = encodingChannel.keyResolver(testVideoId, EncodingChannel.Type.THUMBNAIL);



            //when
            Mono<Void> voidMono = encodingService.encodeThumbnail(testVideoId, copiedFile);



            //then
            StepVerifier.create(voidMono)
                    .verifyComplete();

            EncodingEvent<String> stringEncodingEvent = encodingChannel.getEncodingEvent(testVideoId, EncodingChannel.Type.THUMBNAIL).get();
            assertNotNull(stringEncodingEvent);
            List<String> messages = stringEncodingEvent.getFlux()
                    .doOnNext(System.out::println)
                    .doOnError(s -> fail())
                    .buffer().blockLast();

            assertFalse(messages.isEmpty());

            //파일들이 잘 저장되었는지 확인
            File file = PathManager.VodPath.thumbnailOf(testVideoId).toFile();
            assertTrue(file.exists());
        }

    }


}