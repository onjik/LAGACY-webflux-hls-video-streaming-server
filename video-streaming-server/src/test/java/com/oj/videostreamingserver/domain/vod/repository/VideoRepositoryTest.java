package com.oj.videostreamingserver.domain.vod.repository;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.r2dbc.core.R2dbcEntityTemplate;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.transaction.reactive.TransactionalOperator;

@SpringBootTest
@ContextConfiguration
class VideoRepositoryTest {
    @Autowired
    R2dbcEntityTemplate template;

    @Autowired
    TransactionalOperator transactionalOperator;

}