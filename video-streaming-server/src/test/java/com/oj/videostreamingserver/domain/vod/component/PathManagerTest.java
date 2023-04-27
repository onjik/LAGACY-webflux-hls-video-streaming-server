package com.oj.videostreamingserver.domain.vod.component;

import org.junit.jupiter.api.Test;

import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

class PathManagerTest {

    @Test
    void renameFile(){
        //given
        String ogPath = "test.mp4";
        String newName = "newName";
        String expected = "newName.mp4";

        //when
        String result = PathManager.renameFile(ogPath, newName);

        //then
        assertEquals(expected, result);

    }

}