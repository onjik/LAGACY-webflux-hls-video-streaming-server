package com.oj.videostreamingserver.domain.vod.service;

import com.oj.videostreamingserver.domain.vod.domain.DraftVideo;
import com.oj.videostreamingserver.domain.vod.exception.InvalidTargetPathException;
import com.oj.videostreamingserver.domain.vod.repository.DraftVideoRepository;
import com.oj.videostreamingserver.global.util.FileUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ResourceLoader;
import org.springframework.http.codec.multipart.FilePart;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.io.File;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.UUID;



@Service
@RequiredArgsConstructor
public class VideoFileService {

    @Value("${volume.media}")
    private String mediaVolumeRoot;

    private final DraftVideoRepository draftVideoRepository;
    private final ResourceLoader resourceLoader;

    /**
     *
     * @param filePart input file
     * @param channelId channel Id
     * @return db saved DraftVideo Entity
     * @throws InvalidTargetPathException Exception occurs when build save target path
     * @throws IllegalStateException possibly IllegalStateException if the part isn't a file
     * @throws IllegalArgumentException in case the given entity is null.
     * @throws org.springframework.dao.OptimisticLockingFailureException when the entity uses optimistic locking and has a version attribute with a different value from that found in the persistence store. Also thrown if the entity is assumed to be present but does not exist in the database.
     * @author Onjee Kim
     */
    public Mono<DraftVideo> saveDraft(FilePart filePart,long channelId){
        File destinationFile;
        try {
            //파일 경로 생성
            LocalDateTime now = LocalDateTime.now(); // 현재 시간을 가져옵니다.
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyyMMdd"); // 출력할 형식을 지정합니다.
            String formattedDate = now.format(formatter); // 현재 시간을 지정한 형식으로 출력합니다.
            File directoryFile = Paths.get(mediaVolumeRoot, "draft", formattedDate).toFile();
            directoryFile.mkdirs(); //경로 생성

            //파일 명 생성
            String filename = UUID.randomUUID() + FileUtils.extensionOf(filePart.filename());
            destinationFile = new File(directoryFile,filename);

        } catch (RuntimeException e){
            throw new InvalidTargetPathException();
        }

        //파일 저장 후
        return filePart.transferTo(destinationFile)
                //DB 저장
                .then(draftVideoRepository.save(new DraftVideo(destinationFile.getAbsolutePath(), channelId))
                .onErrorResume(e -> {
                    destinationFile.delete(); //best effort
                    return Mono.error(e);
                }));
    }




}
