package com.oj.videostreamingserver.domain.vod.api;

import com.oj.videostreamingserver.domain.vod.service.VideoFileService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.web.reactive.server.WebTestClient;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
class VodApiTest {

    private WebTestClient webClient;

    @Mock
    private VideoFileService videoFileService;

    @InjectMocks
    private VodApi vodApi;

    @BeforeEach
    public void setup() {
        webClient = WebTestClient.bindToController(vodApi).build();
    }

    @Nested
    @DisplayName("postVideo 메서드")
    class postVideo{



    }

}