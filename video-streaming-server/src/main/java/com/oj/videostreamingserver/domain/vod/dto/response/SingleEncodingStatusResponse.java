package com.oj.videostreamingserver.domain.vod.dto.response;

import com.oj.videostreamingserver.domain.vod.dto.domain.EncodingEvent;
import lombok.Getter;

import java.util.HashMap;
import java.util.Map;

@Getter
public class SingleEncodingStatusResponse {
    private Map<String, EncodingEvent.Status> statusMap;

    public SingleEncodingStatusResponse() {
        this.statusMap = new HashMap<>();
    }
    public void addStatus(String key, EncodingEvent.Status status){
        statusMap.put(key, status);
    }
}
