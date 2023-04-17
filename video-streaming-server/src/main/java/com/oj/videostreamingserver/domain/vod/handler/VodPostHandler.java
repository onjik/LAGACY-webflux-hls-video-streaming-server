package com.oj.videostreamingserver.domain.vod.handler;

import com.oj.videostreamingserver.domain.vod.domain.DraftVideo;
import com.oj.videostreamingserver.domain.vod.dto.OriginalVideoPostResponse;
import com.oj.videostreamingserver.domain.vod.repository.DraftVideoRepository;
import com.oj.videostreamingserver.domain.vod.service.VideoFileService;
import com.oj.videostreamingserver.global.error.ErrorResponse;
import com.oj.videostreamingserver.global.error.exception.InvalidInputValueException;
import com.oj.videostreamingserver.global.error.exception.LocalFileException;
import com.oj.videostreamingserver.global.util.FileUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.http.codec.multipart.FilePart;
import org.springframework.stereotype.Component;
import org.springframework.transaction.reactive.TransactionalOperator;
import org.springframework.web.reactive.function.server.ServerRequest;
import org.springframework.web.reactive.function.server.ServerResponse;
import reactor.core.publisher.Mono;

import java.io.File;
import java.nio.file.InvalidPathException;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.UUID;

/**
 * POST /media : 원본 비디오 포스팅 용 API 핸들러
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class VodPostHandler {

    //constants
    @Value("${volume.media}")
    private String MEDIA_VOLUME_ROOT;
    private final String PATH_SEPARATOR = File.pathSeparator;

    //spring beans
    private final TransactionalOperator transactionalOperator;
    private final VideoFileService videoFileService;
    private final DraftVideoRepository draftVideoRepository;


    /**
     * POST /media : 원본 비디오 포스팅 용 API 핸들러
     * @param request ServerRequest
     * @return ServerResponse 긍정 응답의 경우 body : {@link OriginalVideoPostResponse} 부정 응답의 경우 body : {@link ErrorResponse}
     *
     * @implNote 다음 예외들은 스트림 내부적으로 발생하고 처리됩니다.
     * {@link InvalidInputValueException} 요청에 문제가 있음 : video field, content-type, video filename ,
     * {@link LocalFileException} 저장될 파일의 경로를 지정하는 도중 문제가 생겼습니다. : path mkdir, 경로가 올바르지 않음, 구분자가 잘못됨 등,
     * {@link IllegalStateException} partFile 이 파일이 아닙니다. ,
     * {@link org.springframework.dao.OptimisticLockingFailureException} 논리적으로 발생하지 않는다고 생각하여 처리하지 않음. 오직 INSERT 를 위해서만 save 를 호출하기 떄문
     * {@link org.springframework.dao.DataIntegrityViolationException} DB의 데이터 무결성 옵션을 위반했습니다. : 외래키, not null 등
     *
     */
    public Mono<ServerResponse> postVideo(ServerRequest request) {
        long channelId = 2; //임시로 구현
        return request.multipartData()
                //video 필드 체크
                .filter(multiMap -> multiMap.containsKey("video"))
                .flatMap(multiMap -> Mono.just(multiMap.get("video"))
                        .flatMap(parts -> {
                            if (parts.size() != 1) {
                                return Mono.empty();
                            }
                            return Mono.just(parts.get(0));
                        }))//Mono<Part>
                .ofType(FilePart.class)//Mono<FilePart>
                .switchIfEmpty(Mono.error(new InvalidInputValueException("video", "", "Exactly one video file is required")))
                //content-type 헤더 체크
                .filter(fp -> fp.headers().containsKey("Content-Type") && fp.headers().getFirst("Content-Type").startsWith("video/"))
                .switchIfEmpty(Mono.error(new InvalidInputValueException("Content-Type","","Content-Type that start with video/ is required")))
                //메인 로직
                .as(transactionalOperator::transactional)
                .flatMap(filePart -> this.saveToLocalDraftPath(filePart, channelId)) //Mono<DraftVideo>
                //응답 처리
                .flatMap(draftVideo ->
                                ServerResponse
                                        .ok()
                                        .contentType(MediaType.APPLICATION_JSON)
                                        .bodyValue(new OriginalVideoPostResponse(draftVideo.getId()))
                )
                //예외 처리
                .onErrorResume(ErrorResponse::commonExceptionHandler);
    }

    /**
     * 파일을 임시 디렉토리에 저장하는 메인 로직
     *
     * @param filePart input file
     * @param channelId channel Id
     * @return db saved DraftVideo Entity
     * @throws LocalFileException Exception occurs when build save target path
     * @throws InvalidInputValueException bad request
     * @implNote {@link org.springframework.dao.OptimisticLockingFailureException} 논리적으로 발생하지 않는다고 생각, 왜냐하면 어기서 save 는 오직 INSERT 를 위해 사용되기 때문
     * @author Onjee Kim
     */
    private Mono<DraftVideo> saveToLocalDraftPath(FilePart filePart, long channelId) throws LocalFileException {
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
                .onErrorResume(IllegalStateException.class,e ->  Mono.error(new InvalidInputValueException("video","","given video is not file")))
                //DB 저장
                .then(draftVideoRepository.save(new DraftVideo(destinationFile.getAbsolutePath(), channelId))) //OptimisticLockingFailureException 발생하지 않음 왜냐하면 여기서는 INSERT를 위해서만 save를 사용하기 때문
                //DB 저장중 예외 발생시 로컬 파일 롤백 처리
                .onErrorResume(e -> {
                    destinationFile.delete(); //best effort
                    return Mono.error(e);
                });
    }


}
