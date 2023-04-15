package com.oj.videostreamingserver.domain.vod.repository;

import com.oj.videostreamingserver.domain.vod.domain.DraftVideo;
import org.springframework.data.r2dbc.repository.R2dbcRepository;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface DraftVideoRepository extends ReactiveCrudRepository<DraftVideo,Integer> {
}
