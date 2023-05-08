package com.oj.videostreamingserver.domain.vod.service;

import com.oj.videostreamingserver.domain.vod.component.EncodingChannel;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Sinks;
import reactor.core.scheduler.Schedulers;
import reactor.test.StepVerifier;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class ProcessServiceTest {

    @Test
    void test() {
        //mock
        EncodingChannel encodingChannel = new EncodingChannel();
        ProcessService processService = new ProcessService(encodingChannel);

        //given
        ProcessBuilder processBuilder = new ProcessBuilder("seq", "1", "10");


        UUID videoId = UUID.randomUUID();
        StepVerifier.create(processService.executeAndEmitLog(processBuilder, videoId, EncodingChannel.Type.VIDEO))
                        .assertNext(s -> assertTrue(encodingChannel.contains(videoId, EncodingChannel.Type.VIDEO)))
                        .expectNextCount(9)
                        .verifyComplete();
        assertFalse(encodingChannel.contains(videoId, EncodingChannel.Type.VIDEO));

    }

}