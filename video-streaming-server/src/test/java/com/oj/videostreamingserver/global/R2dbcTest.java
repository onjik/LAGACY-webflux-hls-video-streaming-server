package com.oj.videostreamingserver.global;

import io.r2dbc.spi.ConnectionFactories;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.r2dbc.core.R2dbcEntityTemplate;

public class R2dbcTest {
    R2dbcEntityTemplate template;

    @BeforeEach
    void setUp() {
        template = new R2dbcEntityTemplate(ConnectionFactories.get("r2dbc:mysql://root:qwer1234@localhost:3306/youtube_clone?serverTimezone=UTC"));
    }

    @Test
    void test() {


    }
}
