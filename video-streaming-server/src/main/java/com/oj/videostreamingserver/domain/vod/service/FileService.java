package com.oj.videostreamingserver.domain.vod.service;

import com.oj.videostreamingserver.global.error.exception.InvalidInputValueException;
import com.oj.videostreamingserver.global.error.exception.LocalFileException;
import com.oj.videostreamingserver.global.util.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.codec.multipart.FilePart;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.io.File;
import java.nio.file.InvalidPathException;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.UUID;

@Service
public class FileService {
    @Value("${volume.media}")
    private String MEDIA_VOLUME_ROOT;
    private final String PATH_SEPARATOR = File.pathSeparator;


    /**
     *
     * @param filePart
     * @return if success -> Mono<{@link File}> <br>
     * if error with creating path file -> Mono.error({@link LocalFileException}), <br>
     * if video filename has no dot(".") seperated extension -> Mono.error({@link InvalidInputValueException}), <br>
     * if given filePart is not file -> Mono.error({@link InvalidInputValueException}).
     */
    public Mono<File> saveVideoToDraft(FilePart filePart){
        //파일 경로 생성
        File directoryFile;
        String DRAFT_MIDDLE_PATH = "draft";
        try {
            LocalDateTime now = LocalDateTime.now(); // 현재 시간을 가져옵니다.
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyyMMdd"); // 출력할 형식을 지정합니다.
            String formattedDate = now.format(formatter); // 현재 시간을 지정한 형식으로 출력합니다.
            directoryFile = Paths.get(MEDIA_VOLUME_ROOT, DRAFT_MIDDLE_PATH, formattedDate).toFile();
            directoryFile.mkdirs(); //경로 생성
        } catch (InvalidPathException | UnsupportedOperationException e) {
            return Mono.error(() -> LocalFileException.builder()
                    .pathList(List.of(StringUtils.joinWith(PATH_SEPARATOR, MEDIA_VOLUME_ROOT, DRAFT_MIDDLE_PATH)))
                    .cause(e)
                    .build());
        }

        //파일 저장 경로 생성
        String filename;
        try {
            //파일 명 생성
            filename = UUID.randomUUID() + FileUtils.extensionOf(filePart.filename());
        } catch (IllegalArgumentException e){
            //파일 이름 중 . 으로 구분되는 확장자가 없음
            return Mono.error(()->new InvalidInputValueException("video", filePart.filename(), "can't parse video extension"));
        }
        final File destinationFile = new File(directoryFile,filename);

        return filePart.transferTo(destinationFile)
                .then(Mono.just(destinationFile))
                .onErrorResume(IllegalStateException.class,e ->  Mono.error(new InvalidInputValueException("video","","given video is not file")));

    }
}
