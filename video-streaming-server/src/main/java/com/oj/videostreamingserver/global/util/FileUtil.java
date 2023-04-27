package com.oj.videostreamingserver.global.util;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.UUID;

public class FileUtil {

    /**
     *
     * @param fileName
     * @return
     * @throws IllegalArgumentException filename does not contain '.' seperated extension
     */
    public static String extensionOf(String fileName) throws IllegalArgumentException{
        int lastIndex = fileName.lastIndexOf('.');
        if (lastIndex != -1 && lastIndex < fileName.length() - 1) {
            return fileName.substring(lastIndex);
        } else {
            throw new IllegalArgumentException("filename ( " + fileName + " ) has no extension");
        }
    }

    public static Path rootVodPathOf(String mediaRoot,UUID videoId){
        return Paths.get(mediaRoot,"vod",videoId.toString()).toAbsolutePath();
    }

}
