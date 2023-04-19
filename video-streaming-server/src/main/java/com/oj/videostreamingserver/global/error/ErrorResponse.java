package com.oj.videostreamingserver.global.error;

import com.oj.videostreamingserver.global.dto.ResponseDto;
import com.oj.videostreamingserver.global.error.exception.BusinessException;
import com.oj.videostreamingserver.global.error.exception.DbException;
import com.oj.videostreamingserver.global.error.exception.LocalFileException;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.transaction.CannotCreateTransactionException;
import org.springframework.web.reactive.function.server.ServerResponse;
import reactor.core.publisher.Mono;

import java.util.ArrayList;
import java.util.List;


@Getter
@Slf4j
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ErrorResponse extends ResponseDto {
    private String message;
    private String status;
    private List<FieldError> errors;
    private String code;

    private ErrorResponse(ErrorCode code, List<FieldError> errors){
        super(false);
        this.message = code.getMessage();
        this.status = code.getStatus().toString();
        this.errors = errors;
        this.code = code.getCode();
    }

    private ErrorResponse(ErrorCode code){
        super(false);
        this.message = code.getMessage();
        this.status = code.getStatus().toString();
        this.errors = new ArrayList<>(); //if errorsss is null, response empty list instead of null
        this.code = code.getCode();
    }


    /**
     * 예상치 못한 예외의 최종 목적지
     * 이게 발생해서는 안됨
     */
    public static Mono<ServerResponse> of(Exception e){
        log.error("unhandled exception : {}",e.getMessage());
        e.printStackTrace();
        return ServerResponse.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(new ErrorResponse(ErrorCode.INTERNAL_SERVER_ERROR));
    }



    public static Mono<ServerResponse> commonExceptionHandler(Throwable e){
        //분기적 예외 처리
        if (e instanceof DbException){
            if (e.getCause() instanceof DataIntegrityViolationException) {
                // 데이터 무결성 발생 조건 체크
                String message = e.getMessage().toUpperCase();
                if (message.contains("FOREIGN")) { //외래키 관련 : 여기서는 채널 아이디
                    return ErrorResponse.of(ErrorCode.INVALID_INPUT_VALUE); //Bad Request 로 재시도할 기회를 준다
                } else {
                    // 특정되지 않은 예외들
                    log.debug("unhandled DataIntegrityViolationException in vod handler : e.getMessage = {}", e.getMessage());
                    return ErrorResponse.of(ErrorCode.INTERNAL_SERVER_ERROR);
                }
            } else {
                return ErrorResponse.of(ErrorCode.INTERNAL_SERVER_ERROR);
            }
            //다른 DB 예외 처리
        } else if (e instanceof LocalFileException) {
            // 로컬 파일 상에서 문제가 생겻을 경우 생성
            LocalFileException localFileException = (LocalFileException) e;
            // 이것은 외부에 노출되면 안되는 로그입니다. 꼭 파일에 저장하세요
            log.debug("An LocalFileException occurs -> \n" +
                    "    pathList : {} \n" +
                    "    cause : {} \n" +
                    "    call from : {}",
                    StringUtils.join(localFileException.getPathList(),", "),
                    localFileException.getCause().getClass().getName(),
                    localFileException.getCaller().getName());
            return ServerResponse.status(localFileException.getExpectedResponseStatus()).build();

        } else if (e instanceof BusinessException) {
            BusinessException businessException = (BusinessException) e;
            return ServerResponse.status(businessException.getErrorCode().getStatus())
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(new ErrorResponse(businessException.getErrorCode(), businessException.getFieldErrors()));
        } else if (e instanceof CannotCreateTransactionException) {
            //do something
            log.error(e.getMessage());
            return ErrorResponse.of(ErrorCode.INTERNAL_SERVER_ERROR);
        } else if (e instanceof Exception) {
            //아직 처리되지 않은 Exception 이 도착하는 곳
            log.debug("An unhandled exception has occurred -> {} : {}",e.getClass().getName(),e.getMessage());
            return ErrorResponse.of(ErrorCode.INTERNAL_SERVER_ERROR);
        } else {
            log.debug("An unhandled Throwable has occurred (This is a concept that does not include exception.) -> {} : {}",e.getClass().getName(),e.getMessage());
            return ErrorResponse.of(ErrorCode.INTERNAL_SERVER_ERROR);
        }

    }


    public static Mono<ServerResponse> of(ErrorCode code){
        return ServerResponse.status(code.getStatus())
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(new ErrorResponse(code));
    }


    public static Mono<ServerResponse> of(ErrorCode errorCode, List<FieldError> fieldError) {
        return ServerResponse.status(errorCode.getStatus())
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(new ErrorResponse(errorCode,fieldError));
    }
}
