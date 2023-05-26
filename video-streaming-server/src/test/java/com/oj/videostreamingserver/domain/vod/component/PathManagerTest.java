package com.oj.videostreamingserver.domain.vod.component;

import com.oj.videostreamingserver.domain.vod.util.PathManager;
import org.junit.jupiter.api.Test;

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