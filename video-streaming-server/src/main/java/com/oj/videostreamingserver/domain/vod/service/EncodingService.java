package com.oj.videostreamingserver.domain.vod.service;

import com.oj.videostreamingserver.domain.vod.component.PathManager;
import com.oj.videostreamingserver.domain.vod.component.EncodingChannel;
import com.oj.videostreamingserver.domain.vod.domain.VideoMediaEntry;
import com.oj.videostreamingserver.domain.vod.dto.domain.EncodingRequestForm;
import com.oj.videostreamingserver.global.error.exception.InvalidInputValueException;
import com.oj.videostreamingserver.global.error.exception.KernelProcessException;
import lombok.RequiredArgsConstructor;

import net.bramp.ffmpeg.FFprobe;
import net.bramp.ffmpeg.probe.FFmpegProbeResult;
import net.bramp.ffmpeg.probe.FFmpegStream;
import org.springframework.data.r2dbc.core.R2dbcEntityTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.reactive.TransactionalOperator;
import org.springframework.util.Assert;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.publisher.SignalType;
import reactor.core.scheduler.Schedulers;

import java.io.File;
import java.nio.file.Path;
import java.util.*;
import java.util.function.Supplier;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class EncodingService {

    private final FFprobe fFprobe;
    private final ProcessService processService;

    private final R2dbcEntityTemplate template;
    private final TransactionalOperator transactionalOperator;




    /**
     * 주어진 이미지를 16:9 비율로 크롭하고 .jpg 로 변환하여 썸네일로 추출하는 메서드 <br>
     * blocking code 이므로 주의 <br>
     * 비디오 스트림이 없는 이미지는 {@link InvalidInputValueException} 을 {@link Mono#error(Supplier)} 에 감싸서 던집니다. <br>
     * 썸네일 추출에 실패하면 {@link KernelProcessException} 을 {@link Mono#error(Supplier)} 에 감싸서 던집니다.
     * @param videoId 썸네일을 추출할 비디오의 id
     * @param targetFile 썸네일을 추출할 이미지 파일
     * @return 성공하면 true, 실패하면 false
     * @throws IllegalArgumentException 잘못된 인자 전달
     */
    public Mono<Void> encodeThumbnail(UUID videoId, File targetFile) throws IllegalArgumentException{

        Assert.notNull(targetFile,"targetImg must not be null");
        Assert.notNull(videoId,"videoId must not be null");

        return Mono.fromCallable(() -> fFprobe.probe(targetFile.getAbsolutePath()))
                .onErrorResume(e -> Mono.error(new KernelProcessException("ffprobe", List.of(targetFile.getAbsolutePath()), e)))
                .flatMap(probeResult -> {
                    List<FFmpegStream> videoStream = probeResult.getStreams().stream()
                            .filter(stream -> stream.codec_type == FFmpegStream.CodecType.VIDEO)
                            .collect(Collectors.toList());
                    return Mono.just(videoStream);
                })
                .filter(videoStreams -> !videoStreams.isEmpty())
                .switchIfEmpty(Mono.defer(() -> Mono.error(new InvalidInputValueException("videoFile", videoId.toString(), "비디오 파일에 비디오 스트림이 존재하지 않습니다."))))
                .flatMap(videoStreams -> {
                    FFmpegStream videoStream = videoStreams.get(0);
                    //크롭 옵션 설정
                    int originalHeight = videoStream.height;
                    int originalWidth = videoStream.width;
                    //어떻게 자를건지 결정
                    String crop = "crop=w='min(in_w, ceil((in_h*16/9)/2)*2)':h='min(in_h, ceil((in_w*9/16)/2)*2)',scale=1920:1080";
                    //첫번째 프레임 선택
                    String select = "select=eq(n\\,0),";
                    //퀄리티 설정
                    int quality = 3; //1~31, 1이 가장 좋음

                    //FFmpeg 명령어 생성
                    //예시 ffmpeg 명령어 : ffmpeg -i test.jpg -vf "crop=600:374:0:0,scale=1920:1080" -q:v 1 output.jpg
                    Path outputThumbnailPath = PathManager.VodPath.thumbnailOf(videoId);
                    String[] command = {"ffmpeg", "-i", targetFile.getAbsolutePath(),"-y", "-vf", select+crop, "-q:v", String.valueOf(quality) , outputThumbnailPath.toAbsolutePath().toString()};
                    ProcessBuilder processBuilder = new ProcessBuilder(command);

                    //별도의 쓰레드에서 비동기 실행
                    processService.executeAndEmitLog(processBuilder, videoId, EncodingChannel.Type.THUMBNAIL)
                            .subscribeOn(Schedulers.boundedElastic())
                            .subscribe();
                    return Mono.empty();
                });

    }



    /**
     * 비디오 파일을 인코딩하는 메서드 <br>
     * 인코딩을 기다리지는 않고, 별도의 쓰레드에 요청을 보내는 방식으로 실행된다. <br>
     * 이 요청은 bean 으로 등록된 {@link EncodingChannel} 에 중계하는 방식으로 상황을 전달한다. <br>
     * {@link KernelProcessException} 는 프로세스 실행에 실패했을 때, Mono.error 에 감싸져서 방출된다. <br>
     * {@link InvalidInputValueException} 는 주어진 동영상에 비디오 채널이 존재하지 않을 경우 발생한다. <br>
     * @param videoId 비디오 아이디
     * @param ogVideoPath 원본 비디오 파일 경로
     * @param resolutionCandidates 해상도 후보들
     * @return Mono<Void> 비디오 인코딩이 완료되면 완료 신호를 보낸다.
     * @throws IllegalArgumentException 인자중에 null 이나 emptyList 이 있을 경우 발생한다.
     */
    public Mono<FFmpegProbeResult> encodeVideo(UUID videoId, Path ogVideoPath, List<Integer> resolutionCandidates) throws IllegalArgumentException {
        Assert.notNull(videoId, "videoId must not be null");
        Assert.notNull(ogVideoPath, "ogVideoPath must not be null");
        Assert.notNull(resolutionCandidates, "resolutionCandidates must not be null");
        Assert.notEmpty(resolutionCandidates, "resolutionCandidates must not be empty");

        return Mono.fromCallable(() -> fFprobe.probe(ogVideoPath.toAbsolutePath().toString()))
                .onErrorResume(e -> Mono.error(new KernelProcessException("ffprobe", List.of(ogVideoPath.toAbsolutePath().toString()), e)))
                .filter(probeResult -> probeResult.getStreams().stream().anyMatch(stream -> stream.codec_type == FFmpegStream.CodecType.VIDEO))
                .switchIfEmpty(Mono.defer(() -> Mono.error(new InvalidInputValueException("videoFile", videoId.toString(), "비디오 파일에 비디오 스트림이 존재하지 않습니다."))))
                .flatMap(probeResult -> {
                    List<FFmpegStream> audioStream = probeResult.getStreams().stream()
                            .filter(stream -> stream.codec_type == FFmpegStream.CodecType.AUDIO)
                            .collect(Collectors.toList());
                    List<FFmpegStream> videoStream = probeResult.getStreams().stream()
                            .filter(stream -> stream.codec_type == FFmpegStream.CodecType.VIDEO)
                            .collect(Collectors.toList());
                    //일단 어떤 화질을 만들 것인지 선택
                    int height = videoStream.get(0).height;
                    List<Integer> resolutions = resolutionCandidates.stream()
                            .filter(i -> i <= height)
                            .collect(Collectors.toList());
                    //해상도 후보가 없다면 원본 해상도를 사용
                    if (resolutions.isEmpty()) {
                        resolutions.add(height);
                    }
                    //커맨드 만들기
                    String[] command = buildHlsEncodeCommand(resolutions, ogVideoPath.toFile(), PathManager.VodPath.rootOf(videoId), audioStream.size() > 0);
                    ProcessBuilder processBuilder = new ProcessBuilder(command);


                    /*
                    1. 트랜잭션 처리
                    2. 각 해상도에 대한 DB에 저장 (commit 전)
                    3. 모두 완료되면, 프로세스 실행 -> 이 안에는 중계 로직이 들어 있음
                    4. 프로세스 실행이 완료되면, 끝남 (commit)
                     */
                    Mono.just(resolutions)
                            .as(transactionalOperator::transactional)
                            .flatMapMany(Flux::fromIterable)
                            .flatMap(resolution -> {
                                String videoRootPath = PathManager.VodPath.rootOf(videoId).resolve(resolution.toString()).toAbsolutePath().toString();
                                return Mono.just(new VideoMediaEntry(null, resolution, videoRootPath , videoId));
                            })
                            .flatMap(template::insert)
                            .thenMany(processService.executeAndEmitLog(processBuilder, videoId, EncodingChannel.Type.VIDEO))
                            .subscribeOn(Schedulers.boundedElastic())
                            .subscribe();
                    return Mono.just(probeResult);
                });

    }



    //커맨드 만들어주는 도구
    protected String[] buildHlsEncodeCommand(List<Integer> resolutions, File videoFile, Path videoRoot ,boolean audioChannelExist){

        //커맨드 생성
        List<String> commandBuffer = new ArrayList<>();

        String input = videoFile.getAbsolutePath();
        String master = "master.m3u8";
        String seg = videoRoot.resolve("%v").resolve("file_%03d.ts").toAbsolutePath().toString();
        String index = videoRoot.resolve("%v").resolve("index.m3u8").toAbsolutePath().toString();


        List<String> commandPrefix = List.of(
                "ffmpeg", "-i", input,
                "-preset", "veryfast", "-threads", "1",
                "-c:v", "libx264", "-crf", "22", "-c:a", "aac", "-ar", "48000",
                "-f", "hls", "-hls_time", "10", "-hls_playlist_type", "vod", "-hls_list_size", "0", "-hls_flags", "independent_segments"
                );

        commandBuffer.addAll(commandPrefix);
        //필터 추가
        for (int i = 0; i < resolutions.size();i++){
            if (audioChannelExist){
                commandBuffer.addAll(List.of("-map" , "0:v:0" , "-map", "0:a:0"));
            } else {
                commandBuffer.addAll(List.of("-map" , "0:v:0"));
            }
            commandBuffer.addAll(List.of(
                    "-filter:v:"+i, String.format("scale=-2:%d",resolutions.get(i)))
            ); //"-maxrate:v:0 600k"
        }
        //var_stream_map 추가
        commandBuffer.add("-var_stream_map");
        String[] tempArray = new String[resolutions.size()];
        for (int i = 0; i < resolutions.size();i++){
            if (audioChannelExist){
                tempArray[i] = String.format("v:%d,a:%d,name:%d",i,i,resolutions.get(i));
            } else {
                tempArray[i] = String.format("v:%d,name:%d",i,resolutions.get(i));
            }
        }
        //String array를 가운데 하나씩 빈칸을 두고 String으로 합치는 코드
        String varStreamMapValue = Arrays.stream(tempArray).collect(Collectors.joining(" "));

        commandBuffer.add(varStreamMapValue);

        //postFix 추가
        List<String> commandPostfix = List.of(
                "-master_pl_name", master,
                "-hls_segment_filename",seg,index);
        commandBuffer.addAll(commandPostfix);

        return commandBuffer.toArray(new String[0]);
    }

}
