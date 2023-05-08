package com.oj.videostreamingserver.domain.vod.repository;

import com.oj.videostreamingserver.domain.vod.domain.VideoMediaEntry;
import org.springframework.data.repository.reactive.ReactiveSortingRepository;
import reactor.core.publisher.Mono;

import java.util.UUID;

public interface VideoMediaRepository extends ReactiveSortingRepository<VideoMediaEntry,Long> {
}
