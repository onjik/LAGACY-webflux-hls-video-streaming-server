package com.oj.videostreamingserver.domain.vod.dto.response;

import com.oj.videostreamingserver.domain.vod.component.EncodingChannel;
import com.oj.videostreamingserver.domain.vod.dto.domain.EncodingEvent;
import lombok.Getter;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@Getter
public class SingleEncodingStatusResponse {


    private final EncodingEvent.Status entireJobStatus;
    private final Map<EncodingChannel.Type , EncodingEvent.Status> statusMap;

    public SingleEncodingStatusResponse(Map<EncodingChannel.Type, EncodingEvent.Status> statusMap, EncodingEvent.Status entireJobStatus) {
        this.statusMap = statusMap;
        this.entireJobStatus = statusMap.values().stream().allMatch(status -> status.equals(EncodingEvent.Status.READY)) ? EncodingEvent.Status.READY : EncodingEvent.Status.RUNNING;
    }

    public SingleEncodingStatusResponse(EncodingEvent.Status entireJobStatus) {
        this.statusMap = new HashMap<>();
        this.entireJobStatus = entireJobStatus;
    }
}
