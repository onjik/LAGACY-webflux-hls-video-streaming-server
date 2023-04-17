package com.oj.videostreamingserver.domain.vod.service;

import com.oj.videostreamingserver.domain.vod.domain.DraftVideo;
import com.oj.videostreamingserver.domain.vod.repository.DraftVideoRepository;
import com.oj.videostreamingserver.global.error.exception.InvalidInputValueException;
import com.oj.videostreamingserver.global.error.exception.LocalFileException;
import com.oj.videostreamingserver.global.util.FileUtils;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ResourceLoader;
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
@RequiredArgsConstructor
public class VideoFileService {

    @Value("${volume.media}")
    private String MEDIA_VOLUME_ROOT;

    private final String DRAFT_MIDDLE_PATH = "draft";
    private final String PATH_SEPARATOR = File.pathSeparator;

    private final DraftVideoRepository draftVideoRepository;
    private final ResourceLoader resourceLoader;

    /**
     *
     * @param filePart input file
     * @param channelId channel Id
     * @return db saved DraftVideo Entity
     * @throws LocalFileException Exception occurs when build save target path
     * @throws IllegalStateException possibly IllegalStateException if the part isn't a file
     * @throws IllegalArgumentException in case the given entity is null.
     * @throws org.springframework.dao.OptimisticLockingFailureException when the entity uses optimistic locking and has a version attribute with a different value from that found in the persistence store. Also thrown if the entity is assumed to be present but does not exist in the database.
     * @author Onjee Kim
     */
    public Mono<DraftVideo> saveDraft(FilePart filePart,long channelId) throws LocalFileException {
        //파일 경로 생성
        File directoryFile;
        try {
            LocalDateTime now = LocalDateTime.now(); // 현재 시간을 가져옵니다.
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyyMMdd"); // 출력할 형식을 지정합니다.
            String formattedDate = now.format(formatter); // 현재 시간을 지정한 형식으로 출력합니다.
            directoryFile = Paths.get(MEDIA_VOLUME_ROOT, DRAFT_MIDDLE_PATH, formattedDate).toFile();
            directoryFile.mkdirs(); //경로 생성
        } catch (InvalidPathException|UnsupportedOperationException e) {
            throw LocalFileException.builder()
                    .pathList(List.of(StringUtils.joinWith(PATH_SEPARATOR, MEDIA_VOLUME_ROOT, DRAFT_MIDDLE_PATH)))
                    .cause(e)
                    .build();
        }

        //파일 저장 경로 생성
        String filename;
        try {
            //파일 명 생성
            filename = UUID.randomUUID() + FileUtils.extensionOf(filePart.filename());
        } catch (IllegalArgumentException e){
            //파일 이름 중 . 으로 구분되는 확장자가 없음
            throw new InvalidInputValueException("video", filePart.filename(), "can't parse video extension");
        }


        final File destinationFile = new File(directoryFile,filename);
        return filePart
                //로컬에 파일 저장
                .transferTo(destinationFile) // IllegalStateException : if not file
                //DB 저장
                .then(draftVideoRepository.save(new DraftVideo(destinationFile.getAbsolutePath(), channelId))) //OptimisticLockingFailureException
                //DB 저장중 예외 발생시 로컬 파일 롤백 처리
                .onErrorResume(e -> {
                    destinationFile.delete(); //best effort
                    return Mono.error(e);
                });
    }




}
