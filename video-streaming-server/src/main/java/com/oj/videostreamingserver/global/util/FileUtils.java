package com.oj.videostreamingserver.global.util;

public class FileUtils {


    public static String extensionOf(String fileName) throws IllegalArgumentException{
        int lastIndex = fileName.lastIndexOf('.');
        if (lastIndex != -1 && lastIndex < fileName.length() - 1) {
            return fileName.substring(lastIndex);
        } else {
            throw new IllegalArgumentException("filename ( " + fileName + " ) has no extension");
        }
    }
}
