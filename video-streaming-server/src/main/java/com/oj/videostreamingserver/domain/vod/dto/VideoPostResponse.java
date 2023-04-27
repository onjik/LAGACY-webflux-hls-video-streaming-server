package com.oj.videostreamingserver.domain.vod.dto;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class VideoPostResponse {
    //encrypted tmp file path
    private Long videoId;

    public VideoPostResponse(Long tempId) {
        this.videoId = tempId;
    }

    @Override
    public boolean equals(Object obj) {
        if (!(obj instanceof VideoPostResponse)){
            return false;
        }
        return ((VideoPostResponse) obj).videoId == this.videoId;
    }
}
