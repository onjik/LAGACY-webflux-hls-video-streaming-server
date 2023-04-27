package com.oj.videostreamingserver.global.error.exception;

import lombok.Builder;
import lombok.Getter;
import org.springframework.http.HttpStatus;
import org.springframework.lang.NonNull;
import org.springframework.lang.Nullable;

import java.util.List;


/**
 * 로컬 파일 시스템 관련해서 생긴 예외들을 추상화하여 감싸는 예외 <br>
 * 추후 원인 분석을 위해 로깅됩니다.
 */
@Getter
public class LocalSystemException extends RuntimeException{

    private final HttpStatus expectedResponseStatus;
    private final List<String> pathList;
    @Nullable
    private final Throwable cause;
    private final Class<?> caller;

    /**
     *
     * @param expectedResponseStatus 원하는 응답 상태코드, nullable, null 이면 InternalServerError 가 적용됩니다.
     * @param pathList 예외가 발생한 것으로 예상되는 파일 경로, 이는 외부에 노출 시키지 않고, 내부에서 사용하거나, 파일에 남기는 로그에 사용됩니다.
     * @param cause 이 예외가 감싸고 있는 원본 예외
     */
    @Builder
    public LocalSystemException(@Nullable HttpStatus expectedResponseStatus, @NonNull List<String> pathList, @Nullable Throwable cause) {
        this.expectedResponseStatus = (expectedResponseStatus != null) ? expectedResponseStatus : HttpStatus.INTERNAL_SERVER_ERROR;
        this.pathList = pathList;
        this.cause = cause;
        //추후 로그를 통해 개선하기 위해 이게 어디에서 호출되었는지 기록
        this.caller = Thread.currentThread().getStackTrace()[2].getClass();
    }
}
