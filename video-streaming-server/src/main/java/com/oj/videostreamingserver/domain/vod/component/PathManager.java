package com.oj.videostreamingserver.domain.vod.component;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.nio.file.Path;
import java.util.UUID;

@Component
public class PathManager {
    @Value("${volume.media}")
    private String MEDIA_VOLUME_ROOT;

    private static Path mediaRootPath;

    /**
     * 약간의 트릭을 통해 static 필드를 초기화한다.
     */
    @PostConstruct
    protected void init(){
        mediaRootPath = Path.of(MEDIA_VOLUME_ROOT);
    }

    /**
     * 파일 이름 변경을 해주는 메서드, 확장자를 제외하고 파일 이름만 변경한다.
     * @param fullFileName 변경할 파일 전체 이름(확장자 포함)
     * @param targetName 변경할 파일 이름
     * @return 변경된 파일 이름
     * @example renameFile("test.mp4", "newName") -> "newName.mp4"
     */
    public static String renameFile(String fullFileName, String targetName) {
        // 파일 이름 변경
        return fullFileName.replaceAll("^.*?(?=\\.)", targetName);

    }



    public static class VodPath {
        private static final String VOD = "vod";
        private static final String ORIGINAL_THUMBNAIL_NAME = "temp_thumbnail";
        private static final String THUMBNAIL = "thumbnail.jpg";
        private static final String ORIGINAL_VIDEO_NAME = "original";
        private static final String HLS = "hls";
        private static final String DASH = "dash";

        /**
         * 특정 video 의 루트 디렉토리
         * @param videoId 비디오 아이디
         * @return 비디오 루트 디렉토리
         */
        public static Path rootOf(UUID videoId){
            return mediaRootPath.resolve(VOD).resolve(videoId.toString());
        }

        /**
         * 특정 video 의 썸네일 디렉토리
         * @param videoId 비디오 아이디
         * @return 썸네일 디렉토리
         */
        public static Path thumbnailOf(UUID videoId) {
            return rootOf(videoId).resolve(THUMBNAIL);
        }

        public static Path tempThumbnailOf(UUID videoId,String filename) {
            return rootOf(videoId).resolve(renameFile(filename,ORIGINAL_THUMBNAIL_NAME));
        }
        public static Path tempThumbnailOf(UUID videoId) {
            return rootOf(videoId).resolve(ORIGINAL_THUMBNAIL_NAME+".jpg");
        }

        /**
         * 특정 video 의 비디오 파일 디렉토리
         * @param videoId 비디오 아이디
         * @return 비디오 파일 디렉토리
         */
        public static Path ogVideoOf(UUID videoId, String filename) {
            return rootOf(videoId).resolve(renameFile(filename, ORIGINAL_VIDEO_NAME));
        }




    }


}
