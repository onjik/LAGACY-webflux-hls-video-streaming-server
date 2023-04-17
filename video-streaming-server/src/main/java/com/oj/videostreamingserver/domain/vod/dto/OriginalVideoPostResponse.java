package com.oj.videostreamingserver.domain.vod.dto;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class OriginalVideoPostResponse {
    //encrypted tmp file path
    private Long tempId;

    public OriginalVideoPostResponse(Long tempId) {
        this.tempId = tempId;
    }

    @Override
    public boolean equals(Object obj) {
        if (!(obj instanceof OriginalVideoPostResponse)){
            return false;
        }
        return ((OriginalVideoPostResponse) obj).tempId == this.tempId;
    }
}
