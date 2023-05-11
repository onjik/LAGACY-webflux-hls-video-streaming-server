package com.oj.videostreamingserver.domain.vod.dto.response;

import lombok.Getter;

@Getter
public class VideoPostResponse {
    private String videoId;
    private String message;

    //비디오 길이 (초 단위)
    private double videoLength;

    public VideoPostResponse(String videoId, double videoLength) {
        this.videoId = videoId;
        this.videoLength = videoLength;
        this.message = "video successfully registered in encoding queue";
    }

}
