package com.oj.videostreamingserver.global.error.exception;

import org.springframework.http.HttpStatus;

import java.util.List;


public class KernelProcessException extends LocalSystemException{

    private final String kernelName;

    public String getKernelName() {
        return kernelName;
    }

    /**
     * @param expectedResponseStatus 원하는 응답 상태코드, nullable, null 이면 InternalServerError 가 적용됩니다.
     * @param pathList               예외가 발생한 것으로 예상되는 파일 경로, 이는 외부에 노출 시키지 않고, 내부에서 사용하거나, 파일에 남기는 로그에 사용됩니다.
     * @param cause                  이 예외가 감싸고 있는 원본 예외
     */
    public KernelProcessException(String kernelName, HttpStatus expectedResponseStatus, List<String> pathList, Throwable cause) {
        super(expectedResponseStatus, pathList, cause);
        this.kernelName = kernelName;
    }

    public KernelProcessException(String kernelName, List<String> pathList, Throwable cause) {
        this(kernelName, HttpStatus.INTERNAL_SERVER_ERROR, pathList, cause);
    }

}
