package com.oj.videostreamingserver.domain.vod.domain;

import lombok.Getter;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

import java.util.UUID;

/*
-- -----------------------------------------------------
-- Table `youtube_clone`.`video_media`
-- -----------------------------------------------------
CREATE TABLE IF NOT EXISTS `youtube_clone`.`video_media` (
  `video_media_Id` INT NOT NULL,
  `resolution` INT NULL,
  `video_id` BINARY(16) NOT NULL,
  `video_root_path` VARCHAR(255) NULL,
  PRIMARY KEY (`video_media_Id`),
  INDEX `fk_video_media_video1_idx` (`video_id` ASC) VISIBLE,
  CONSTRAINT `fk_video_media_video1`
    FOREIGN KEY (`video_id`)
    REFERENCES `youtube_clone`.`video` (`video_id`)
    ON DELETE NO ACTION
    ON UPDATE NO ACTION)
ENGINE = InnoDB;
 */


@Getter
@Table("video_media")
public class VideoMediaEntry {

    //DB 자동 증가 옵션
    @Id
    @Column("video_media_id")
    private Integer videoMediaId;

    @Column("resolution")
    private Integer resolution;

    @Column("video_root_path")
    private String videoRootPath;

    @Column("video_id")
    private UUID videoId;

    public VideoMediaEntry(Integer videoMediaId, Integer resolution, String videoRootPath, UUID videoId) {
        this.videoMediaId = videoMediaId;
        this.resolution = resolution;
        this.videoRootPath = videoRootPath;
        this.videoId = videoId;
    }

}
