package com.oj.videostreamingserver.domain.vod.service;

import com.oj.videostreamingserver.domain.vod.component.PathManager;
import com.oj.videostreamingserver.domain.vod.dto.EncodingEvent;
import com.oj.videostreamingserver.domain.vod.component.EncodingChannel;
import com.oj.videostreamingserver.domain.vod.component.IndependentExecutor;
import com.oj.videostreamingserver.domain.vod.dto.EncodingRequestForm;
import com.oj.videostreamingserver.global.error.exception.InvalidInputValueException;
import com.oj.videostreamingserver.global.error.exception.KernelProcessException;
import lombok.RequiredArgsConstructor;

import net.bramp.ffmpeg.FFprobe;
import net.bramp.ffmpeg.probe.FFmpegProbeResult;
import net.bramp.ffmpeg.probe.FFmpegStream;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.Path;
import java.util.*;
import java.util.function.Supplier;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class EncodingService {

    @Value("${path.ffmpeg}")
    private String FFMPEG_KERNEL_PATH;

    private final FFprobe fFprobe;

    private final EncodingChannel encodingChannel;

    private final IndependentExecutor independentExecutor;


    /**
     * 비디오 파일의 첫번째 프레임을 썸네일로 추출하는 메서드 <br>
     * 추출된 썸네일은 원본 비디오 파일과 같은 디렉토리에 thumbnail.jpg로 저장된다.
     *
     * @param videoFilePath video file path
     * @param outputImagePath image save path
     * @throws KernelProcessException if ffmpeg process failed
     */
    public void extractFirstFrame(Path videoFilePath, Path outputImagePath) throws KernelProcessException{
        //Process 생성
        ProcessBuilder processBuilder = new ProcessBuilder();

        //FFmpeg 명령어 생성
        String[] command = {"ffmpeg", "-i", videoFilePath.toAbsolutePath().toString(), "-vframes", "1", outputImagePath.toAbsolutePath().toString()};
        processBuilder.command(command);

        //blocking code
        try {
            //실행
            Process process = processBuilder.start();
            //실행이 끝날 때까지 대기
            process.waitFor(); //blocking : To do 추후 비동기로 바꾸기

            //process 가 성공적으로 종료됬는지 체크하고 만약 실패했으면 예외 던지기
            if (process.exitValue() != 0){
                throw new KernelProcessException("ffmpeg",List.of(videoFilePath.toString(),outputImagePath.toString()),null); //To do : 추후 세밀하게 처리 필요
            }
        } catch (IOException e) {
            throw new KernelProcessException("ffmpeg",List.of(videoFilePath.toString(),outputImagePath.toString()),e); //To do : 추후 세밀하게 처리 필요
        } catch (InterruptedException e) {
            //인터럽트 시에는 그냥 깨어나기만
        }

    }

    /**
     * 주어진 이미지를 16:9 비율로 크롭하고 .jpg 로 변환하여 썸네일로 추출하는 메서드 <br>
     * blocking code 이므로 주의 <br>
     * 비디오 스트림이 없는 이미지는 {@link InvalidInputValueException} 을 {@link Mono#error(Supplier)} 에 감싸서 던집니다. <br>
     * 썸네일 추출에 실패하면 {@link KernelProcessException} 을 {@link Mono#error(Supplier)} 에 감싸서 던집니다.
     * @param form EncodingRequestForm
     * @return 성공하면 true, 실패하면 false
     * @throws IllegalArgumentException 잘못된 인자 전달
     */
    public Mono<Void> encodeThumbnail(EncodingRequestForm form) throws IllegalArgumentException{
        return this.encodeThumbnail(form.getVideoId(), form.getTempThumbnailPath().toFile());
    }

    /**
     * 주어진 이미지를 16:9 비율로 크롭하고 .jpg 로 변환하여 썸네일로 추출하는 메서드 <br>
     * blocking code 이므로 주의 <br>
     * 비디오 스트림이 없는 이미지는 {@link InvalidInputValueException} 을 {@link Mono#error(Supplier)} 에 감싸서 던집니다. <br>
     * 썸네일 추출에 실패하면 {@link KernelProcessException} 을 {@link Mono#error(Supplier)} 에 감싸서 던집니다.
     * @param videoId 썸네일을 추출할 비디오의 id
     * @param targetImg 썸네일을 추출할 이미지 파일
     * @return 성공하면 true, 실패하면 false
     * @throws IllegalArgumentException 잘못된 인자 전달
     */
    public Mono<Void> encodeThumbnail(UUID videoId, File targetImg) throws IllegalArgumentException{
        Assert.notNull(targetImg,"targetImg must not be null");
        Assert.notNull(videoId,"videoId must not be null");
        Assert.isTrue(targetImg.exists(),"targetImg must exist");

        return Mono.<Void>fromRunnable(() -> {
            //기본 정보 읽어오기
            Optional<FFmpegStream> optionalVideoStream;
            try {
                optionalVideoStream = fFprobe.probe(targetImg.getAbsolutePath()).getStreams().stream()
                        .filter(s -> s.codec_type.equals(FFmpegStream.CodecType.VIDEO))
                        .findFirst();
            } catch (IOException e) {
                throw new KernelProcessException("ffprobe",List.of(targetImg.getAbsolutePath()),e);
            }

            //비디오 스트림이 없으면 에러
            if (optionalVideoStream.isEmpty()){
                throw new InvalidInputValueException("thumbnailImg","","Given image has no video channel");
            }

            //비디오 스트림 정보 분석
            FFmpegStream videoStream = optionalVideoStream.orElseThrow(() -> new InvalidInputValueException("thumbnailImg","","Given image has no video channel"));
            //크롭 옵션 설정
            int originalHeight = videoStream.height;
            int originalWidth = videoStream.width;
            int dx, dy, newHeight, newWidth;
            //어떻게 자를건지 결정
            if (originalHeight > originalWidth){
                newWidth = originalWidth;
                newHeight = Math.multiplyExact(originalWidth,16) * 9;
                dx = 0;
                dy = (originalHeight-newHeight)/2;
            } else {
                newWidth = Math.multiplyExact(originalHeight,9) * 16;
                newHeight = originalHeight;
                dx = (originalWidth - newWidth)/2;
                dy = 0;
            }

            //FFmpeg 명령어 생성
            //예시 ffmpeg 명령어 : ffmpeg -i test.jpg -vf "crop=600:374:0:0,scale=1920:1080" -q:v 1 output.jpg
            String[] command = {"ffmpeg", "-i", targetImg.getAbsolutePath(), "-vf", String.format("crop=%d:%d:%d:%d", newWidth, newHeight, dx, dy), "-q:v", "1", PathManager.VodPath.thumbnailOf(videoId).toAbsolutePath().toString()};

            ProcessBuilder pb = new ProcessBuilder();
            pb.command(command);

            //중계 등록
            String broadCastKey = encodingChannel.registerEvent(videoId, EncodingChannel.Type.THUMBNAIL);

            //독립적으로 돌아가는 인코딩 쓰레드에 등록한다.
            independentExecutor.executeAndBroadCast(pb,encodingChannel,broadCastKey);
            return;
        }).subscribeOn(Schedulers.boundedElastic()); //blocking 코드이므로 별도 쓰레드에서 실행한다.


    }

    /**
     * 비디오 파일을 인코딩하는 메서드 <br>
     * 인코딩을 기다리지는 않고, 별도의 쓰레드에 요청을 보내는 방식으로 실행된다. <br>
     * 이 요청은 bean 으로 등록된 {@link EncodingChannel} 에 중계하는 방식으로 상황을 전달한다. <br>
     * {@link KernelProcessException} 는 프로세스 실행에 실패했을 때, Mono.error 에 감싸져서 방출된다. <br>
     * {@link InvalidInputValueException} 는 주어진 동영상에 비디오 채널이 존재하지 않을 경우 발생한다. <br>
     * @param form 인코딩 요청 폼
     * @return Mono<Void> 비디오 인코딩이 완료되면 완료 신호를 보낸다.
     * @throws IllegalArgumentException 인자중에 null 이나 emptyList 이 있을 경우 발생한다.
     * @see EncodingService#encodeVideo(UUID, Path, List)
     */
    public Mono<EncodingRequestForm> encodeVideo(EncodingRequestForm form) throws IllegalArgumentException {
        return this.encodeVideo(form.getVideoId(), form.getOgVideoPath(), form.getResolutionCandidates())
                .then(Mono.just(form));
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
    public Mono<Void> encodeVideo(UUID videoId, Path ogVideoPath, List<Integer> resolutionCandidates) throws IllegalArgumentException {
        Assert.notNull(videoId, "videoId must not be null");
        Assert.notNull(ogVideoPath, "ogVideoPath must not be null");
        Assert.notNull(resolutionCandidates, "resolutionCandidates must not be null");
        Assert.notEmpty(resolutionCandidates, "resolutionCandidates must not be empty");

        return Mono.<Void>fromRunnable(() -> {
            //동영상 비디오/오디오 스트림 추출
            FFmpegProbeResult probeResult = null;
            try {
                probeResult = fFprobe.probe(ogVideoPath.toAbsolutePath().toString());
            } catch (IOException e) {
                throw new KernelProcessException("probe",List.of(ogVideoPath.toAbsolutePath().toString()),e);
            }
            List<FFmpegStream> audioStream = probeResult.getStreams().stream()
                    .filter(stream -> stream.codec_type == FFmpegStream.CodecType.AUDIO)
                    .collect(Collectors.toList());
            List<FFmpegStream> videoStream = probeResult.getStreams().stream()
                    .filter(stream -> stream.codec_type == FFmpegStream.CodecType.VIDEO)
                    .collect(Collectors.toList());
            if (videoStream.isEmpty()) {
                throw new InvalidInputValueException("videoFile",videoId.toString(),"비디오 파일에 비디오 스트림이 존재하지 않습니다.");
            }

            //일단 어떤 화질을 만들 것인지 선택
            int height = videoStream.get(0).height;
            List<Integer> resolutions = resolutionCandidates.stream()
                    .filter(i -> i <= height)
                    .collect(Collectors.toList());

            //커맨드 만들기
            String[] command = buildHlsEncodeCommand(resolutions, ogVideoPath.toFile(),PathManager.VodPath.rootOf(videoId),audioStream.size() > 0);

            ProcessBuilder pb = new ProcessBuilder(command);
            pb.command(command);

            //중계를 등록합니다.
            String broadCastKey = encodingChannel.registerEvent(videoId, EncodingChannel.Type.VIDEO);

            //독립적으로 돌아가는 인코딩 쓰레드에 등록한다.
            independentExecutor.executeAndBroadCast(pb,encodingChannel,broadCastKey);
            return;
        }).subscribeOn(Schedulers.boundedElastic()); //blocking 코드이므로 별도 쓰레드에서 실행한다.
    }



    //커맨드 만들어주는 도구
    private String[] buildHlsEncodeCommand(List<Integer> resolutions, File videoFile, Path videoRoot ,boolean audioChannelExist){
        //커맨드 생성
        List<String> commandBuffer = new ArrayList<>();

        String input = videoFile.getAbsolutePath();
        String master = videoRoot.resolve("master.m3u8").toAbsolutePath().toString();
        String seg = videoRoot.resolve("%v").resolve("file_%03d.ts").toAbsolutePath().toString();
        String index = videoRoot.resolve("%v").resolve("index.m3u8").toAbsolutePath().toString();


        List<String> commandPrefix = List.of(
                "ffmpeg","-i",input,
                "-preset", "veryfast", "-threads", "0",
                "-c:v", "libx264", "-crf", "22", "-c:a", "aac", "-ar", "48000",
                "-f", "hls", "-hls_time", "10", "-hls_playlist_type", "vod", "-hls_list_size", "0", "-hls_flags", "independent_segments"
                );

        commandBuffer.addAll(commandPrefix);
        //필터 추가
        for (int i = 0; i < resolutions.size();i++){
            if (audioChannelExist){
                commandBuffer.addAll(List.of("-map" + "0:v:0" + "-map"+ "0:a:0"));
            } else {
                commandBuffer.addAll(List.of("-map" + "0:v:0"));
            }
            commandBuffer.addAll(List.of(
                    "-filter:v:"+i, String.format("scale=-2:%d:force_original_aspect_ratio=decrease",resolutions.get(i)))
            ); //"-maxrate:v:0 600k"
        }
        //var_stream_map 추가
        commandBuffer.add("-var_stream_map");
        String build = "";
        for (int i = 0; i < resolutions.size();i++){
            if (audioChannelExist){
                build = String.join(" ",build,String.format("v:%d,a:%d,name:%d",i,i,resolutions.get(i)));
            } else {
                build = String.join(" ",build,String.format("v:%d,name:%d",i,resolutions.get(i)));
            }
        }
        commandBuffer.add("\""+build+"\"");

        //postFix 추가
        List<String> commandPostfix = List.of(
                "-master_pl_name", master,
                "-hls_segment_filename",seg,index);
        commandBuffer.addAll(commandPostfix);

        return commandBuffer.toArray(new String[0]);
    }

}
