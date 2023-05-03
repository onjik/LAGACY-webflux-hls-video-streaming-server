package com.oj.videostreamingserver.domain.vod.dto.domain;

import lombok.Builder;
import lombok.Getter;
import org.springframework.util.Assert;

import java.nio.file.Path;
import java.util.List;
import java.util.UUID;

/**
 * 비디오 인코딩을 요청하는 양식
 */
@Getter
public class EncodingRequestForm {
    private UUID videoId;
    private Path ogVideoPath;
    private List<Integer> resolutionCandidates;

    @Builder
    public EncodingRequestForm(UUID videoId, Path ogVideoPath, List<Integer> resolutionCandidates) {
        Assert.notNull(videoId, "videoId must not be null");
        Assert.notNull(ogVideoPath, "ogVideoPath must not be null");
        Assert.notNull(resolutionCandidates, "resolutionCandidates must not be null");

        this.videoId = videoId;
        this.ogVideoPath = ogVideoPath;
        this.resolutionCandidates = resolutionCandidates;
    }
}
