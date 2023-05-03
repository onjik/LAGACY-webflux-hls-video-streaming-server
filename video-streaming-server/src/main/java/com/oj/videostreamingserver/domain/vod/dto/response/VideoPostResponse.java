package com.oj.videostreamingserver.domain.vod.dto.response;

import lombok.Getter;

@Getter
public class VideoPostResponse {
    private String videoId;
    private String message;

    public VideoPostResponse(String videoId) {
        this.videoId = videoId;
        this.message = "video successfully registered in encoding queue";
    }

}
