package com.oj.videostreamingserver.domain.vod.domain;

import lombok.Getter;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Table("draft_video")
public class DraftVideo {

    @Id
    @Column("draft_id")
    private Integer id;

    @Column("file_path")
    private String filePath;

    @Column("owner")
    private Long channelId;

    @CreatedDate
    @Column("created_date")
    private LocalDateTime createdAt;

    public DraftVideo(String filePath, Long channelId) {
        this.filePath = filePath;
        this.channelId = channelId;
    }
}
