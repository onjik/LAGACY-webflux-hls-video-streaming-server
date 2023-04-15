package com.oj.videostreamingserver.domain.vod.repository;

import com.oj.videostreamingserver.domain.vod.domain.DraftVideo;
import com.oj.videostreamingserver.global.util.Transaction;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.r2dbc.connection.R2dbcTransactionManager;
import org.springframework.test.annotation.Rollback;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.transaction.annotation.Transactional;
import reactor.test.StepVerifier;

import static org.junit.jupiter.api.Assertions.*;

@ContextConfiguration
@SpringBootTest
class DraftVideoRepositoryTest {

    @Autowired
    DraftVideoRepository repository;

    @Test
    void readsAllEn(){
        repository.save(new DraftVideo("/ttke/dd", 1L))
                .as(Transaction::withRollBack)
                        .as(StepVerifier::create)
                .expectNextCount(1)
                                .verifyComplete();

        repository.findAll().log()
                .as(StepVerifier::create)
                .expectNextCount(7)
                .verifyComplete();
    }

    @Test
    @DisplayName("잘못된 foreign key")
    void testAA(){
        repository.save(new DraftVideo("/dd/fd",2L)).log()
                .as(Transaction::withRollBack)
                .as(StepVerifier::create)
                .verifyError(DataIntegrityViolationException.class);
    }
}