package com.oj.videostreamingserver.global.dto;


import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;

/**
 * 모든 응답 메시지는 이 DTO를 상속해야 합니다.
 * 이는 isSuccess 의 포함을 강제하기 위함입니다.
 */
@RequiredArgsConstructor(access = AccessLevel.PROTECTED)
public abstract class ResponseDto {
    protected final boolean isSuccess;

}
