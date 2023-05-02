-- MySQL Workbench Forward Engineering

SET @OLD_UNIQUE_CHECKS=@@UNIQUE_CHECKS, UNIQUE_CHECKS=0;
SET @OLD_FOREIGN_KEY_CHECKS=@@FOREIGN_KEY_CHECKS, FOREIGN_KEY_CHECKS=0;
SET @OLD_SQL_MODE=@@SQL_MODE, SQL_MODE='ONLY_FULL_GROUP_BY,STRICT_TRANS_TABLES,NO_ZERO_IN_DATE,NO_ZERO_DATE,ERROR_FOR_DIVISION_BY_ZERO,NO_ENGINE_SUBSTITUTION';

-- -----------------------------------------------------
-- Schema youtube_clone
-- -----------------------------------------------------

-- -----------------------------------------------------
-- Schema youtube_clone
-- -----------------------------------------------------
CREATE SCHEMA IF NOT EXISTS `youtube_clone` DEFAULT CHARACTER SET utf8 ;
USE `youtube_clone` ;

-- -----------------------------------------------------
-- Table `youtube_clone`.`member`
-- -----------------------------------------------------
CREATE TABLE IF NOT EXISTS `youtube_clone`.`member` (
  `member_id` BIGINT(20) NOT NULL,
  `email` VARCHAR(100) NOT NULL,
  `name` NVARCHAR(50) NOT NULL,
  `provider` VARCHAR(100) NOT NULL,
  PRIMARY KEY (`member_id`),
  UNIQUE INDEX `email_UNIQUE` (`email` ASC) VISIBLE)
ENGINE = InnoDB;


-- -----------------------------------------------------
-- Table `youtube_clone`.`channel`
-- -----------------------------------------------------
CREATE TABLE IF NOT EXISTS `youtube_clone`.`channel` (
  `channel_id` BIGINT(20) NOT NULL,
  `title` VARCHAR(100) NOT NULL,
  `description` VARCHAR(255) NULL,
  `profile_img` BLOB NULL,
  `member_id` BIGINT(20) NOT NULL,
  PRIMARY KEY (`channel_id`),
  UNIQUE INDEX `title_UNIQUE` (`title` ASC) VISIBLE,
  INDEX `fk_channel_member_idx` (`member_id` ASC) VISIBLE,
  CONSTRAINT `fk_channel_member`
    FOREIGN KEY (`member_id`)
    REFERENCES `youtube_clone`.`member` (`member_id`)
    ON DELETE NO ACTION
    ON UPDATE NO ACTION)
ENGINE = InnoDB;


-- -----------------------------------------------------
-- Table `youtube_clone`.`subscribe`
-- -----------------------------------------------------
CREATE TABLE IF NOT EXISTS `youtube_clone`.`subscribe` (
  `subscribe_id` BIGINT(20) NOT NULL,
  `subscriber` BIGINT(20) NOT NULL,
  `channel` BIGINT(20) NOT NULL,
  PRIMARY KEY (`subscribe_id`),
  INDEX `fk_subscribe_channel1_idx` (`subscriber` ASC) VISIBLE,
  INDEX `fk_subscribe_channel2_idx` (`channel` ASC) VISIBLE,
  CONSTRAINT `fk_subscribe_channel1`
    FOREIGN KEY (`subscriber`)
    REFERENCES `youtube_clone`.`channel` (`channel_id`)
    ON DELETE NO ACTION
    ON UPDATE NO ACTION,
  CONSTRAINT `fk_subscribe_channel2`
    FOREIGN KEY (`channel`)
    REFERENCES `youtube_clone`.`channel` (`channel_id`)
    ON DELETE NO ACTION
    ON UPDATE NO ACTION)
ENGINE = InnoDB;


-- -----------------------------------------------------
-- Table `youtube_clone`.`video`
-- -----------------------------------------------------
CREATE TABLE IF NOT EXISTS `youtube_clone`.`video` (
  `video_id` BINARY(16) NOT NULL,
  `thumb_img` BLOB NOT NULL,
  `title` VARCHAR(100) NOT NULL,
  `description` VARCHAR(255) NOT NULL,
  `created_time` DATETIME NOT NULL,
  `updated_time` DATETIME NOT NULL,
  `view_count` INT NOT NULL,
  `channel_id` BIGINT(20) NOT NULL,
  `status` ENUM('public', 'private', 'draft') NOT NULL,
  PRIMARY KEY (`video_id`),
  INDEX `fk_video_info_channel1_idx` (`channel_id` ASC) VISIBLE,
  CONSTRAINT `fk_video_info_channel1`
    FOREIGN KEY (`channel_id`)
    REFERENCES `youtube_clone`.`channel` (`channel_id`)
    ON DELETE NO ACTION
    ON UPDATE NO ACTION)
ENGINE = InnoDB;


-- -----------------------------------------------------
-- Table `youtube_clone`.`comment`
-- -----------------------------------------------------
CREATE TABLE IF NOT EXISTS `youtube_clone`.`comment` (
  `comment_id` BIGINT(20) NOT NULL,
  `contents` VARCHAR(255) NOT NULL,
  `created_time` DATETIME NOT NULL,
  `updated_time` DATETIME NOT NULL,
  `channel_id` BIGINT(20) NOT NULL,
  `root_comment_id` BIGINT(20) NOT NULL,
  `video_id` BINARY(16) NOT NULL,
  PRIMARY KEY (`comment_id`),
  INDEX `fk_comment_channel1_idx` (`channel_id` ASC) VISIBLE,
  INDEX `fk_comment_comment1_idx` (`root_comment_id` ASC) VISIBLE,
  INDEX `fk_comment_video1_idx` (`video_id` ASC) VISIBLE,
  CONSTRAINT `fk_comment_channel1`
    FOREIGN KEY (`channel_id`)
    REFERENCES `youtube_clone`.`channel` (`channel_id`)
    ON DELETE NO ACTION
    ON UPDATE NO ACTION,
  CONSTRAINT `fk_comment_comment1`
    FOREIGN KEY (`root_comment_id`)
    REFERENCES `youtube_clone`.`comment` (`comment_id`)
    ON DELETE NO ACTION
    ON UPDATE NO ACTION,
  CONSTRAINT `fk_comment_video1`
    FOREIGN KEY (`video_id`)
    REFERENCES `youtube_clone`.`video` (`video_id`)
    ON DELETE NO ACTION
    ON UPDATE NO ACTION)
ENGINE = InnoDB;


-- -----------------------------------------------------
-- Table `youtube_clone`.`video_like`
-- -----------------------------------------------------
CREATE TABLE IF NOT EXISTS `youtube_clone`.`video_like` (
  `video_like_id` BIGINT(20) NOT NULL,
  `channel_id` BIGINT(20) NOT NULL,
  `video_id` BINARY(16) NOT NULL,
  PRIMARY KEY (`video_like_id`),
  INDEX `fk_video_like_channel1_idx` (`channel_id` ASC) VISIBLE,
  INDEX `fk_video_like_video1_idx` (`video_id` ASC) VISIBLE,
  CONSTRAINT `fk_video_like_channel1`
    FOREIGN KEY (`channel_id`)
    REFERENCES `youtube_clone`.`channel` (`channel_id`)
    ON DELETE NO ACTION
    ON UPDATE NO ACTION,
  CONSTRAINT `fk_video_like_video1`
    FOREIGN KEY (`video_id`)
    REFERENCES `youtube_clone`.`video` (`video_id`)
    ON DELETE NO ACTION
    ON UPDATE NO ACTION)
ENGINE = InnoDB;


-- -----------------------------------------------------
-- Table `youtube_clone`.`comment_like`
-- -----------------------------------------------------
CREATE TABLE IF NOT EXISTS `youtube_clone`.`comment_like` (
  `comment_like_id` BIGINT(20) NOT NULL,
  `channel_id` BIGINT(20) NOT NULL,
  `comment_id` BIGINT(20) NOT NULL,
  PRIMARY KEY (`comment_like_id`),
  INDEX `fk_comment_like_channel1_idx` (`channel_id` ASC) VISIBLE,
  INDEX `fk_comment_like_comment1_idx` (`comment_id` ASC) VISIBLE,
  CONSTRAINT `fk_comment_like_channel1`
    FOREIGN KEY (`channel_id`)
    REFERENCES `youtube_clone`.`channel` (`channel_id`)
    ON DELETE NO ACTION
    ON UPDATE NO ACTION,
  CONSTRAINT `fk_comment_like_comment1`
    FOREIGN KEY (`comment_id`)
    REFERENCES `youtube_clone`.`comment` (`comment_id`)
    ON DELETE NO ACTION
    ON UPDATE NO ACTION)
ENGINE = InnoDB;


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


SET SQL_MODE=@OLD_SQL_MODE;
SET FOREIGN_KEY_CHECKS=@OLD_FOREIGN_KEY_CHECKS;
SET UNIQUE_CHECKS=@OLD_UNIQUE_CHECKS;
