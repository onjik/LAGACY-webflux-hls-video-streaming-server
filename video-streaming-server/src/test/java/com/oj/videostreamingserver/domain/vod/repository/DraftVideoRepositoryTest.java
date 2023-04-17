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
    @DisplayName("잘못된 foreign key")
    void foreignKeyViolation(){
        repository.save(new DraftVideo("/dd/fd",2L)).log()
                .as(Transaction::withRollBack)
                .as(StepVerifier::create)
                .verifyError(DataIntegrityViolationException.class);
    }
}