package com.joprelys.backend.auth.session.application;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.joprelys.backend.auth.infrastructure.persistence.UserAccountEntity;
import com.joprelys.backend.auth.infrastructure.persistence.UserAccountRepository;
import com.joprelys.backend.auth.session.infrastructure.persistence.AuthSessionEntity;
import com.joprelys.backend.auth.session.infrastructure.persistence.AuthSessionRepository;
import com.joprelys.backend.clinic.infrastructure.persistence.OrganizationEntity;
import com.joprelys.backend.clinic.infrastructure.persistence.OrganizationRepository;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest
@ActiveProfiles("test")
class AuthSessionRotationConcurrencyTest {

    private static final SessionClientMetadata METADATA = new SessionClientMetadata(
            "WEB", "Concurrency-Test", "127.0.0.0/24");

    @Autowired IssueAuthSessionUseCase issueAuthSessionUseCase;
    @Autowired RefreshAuthSessionUseCase refreshAuthSessionUseCase;
    @Autowired AuthSessionRepository sessionRepository;
    @Autowired UserAccountRepository userAccountRepository;
    @Autowired OrganizationRepository organizationRepository;

    private ExecutorService executor;

    @BeforeEach
    void setUp() {
        sessionRepository.deleteAll();
        userAccountRepository.deleteAll();
        organizationRepository.deleteAll();
        executor = Executors.newFixedThreadPool(2);
    }

    @AfterEach
    void tearDown() {
        executor.shutdownNow();
    }

    @Test
    void shouldAllowExactlyOneRefreshWhenSameTokenIsSubmittedConcurrently() throws Exception {
        OrganizationEntity organization = organizationRepository.save(new OrganizationEntity(
                "Clinique Concurrence",
                "concurrency@joprelys.local",
                "+237600000002",
                "Rue A",
                "Douala"));
        UserAccountEntity user = new UserAccountEntity(
                "concurrency.user@joprelys.local",
                "Concurrency User",
                "AGENT_ACCUEIL",
                "hash");
        user.setOrganizationId(organization.getId());
        user = userAccountRepository.save(user);
        IssuedAuthSession issued = issueAuthSessionUseCase.issue(user, METADATA);

        CountDownLatch ready = new CountDownLatch(2);
        CountDownLatch start = new CountDownLatch(1);
        Future<Boolean> first = executor.submit(() -> rotate(issued.refreshToken(), ready, start));
        Future<Boolean> second = executor.submit(() -> rotate(issued.refreshToken(), ready, start));
        ready.await();
        start.countDown();

        int successes = Boolean.TRUE.equals(first.get()) ? 1 : 0;
        successes += Boolean.TRUE.equals(second.get()) ? 1 : 0;
        assertEquals(1, successes);

        List<AuthSessionEntity> allSessions = sessionRepository.findAll();
        assertEquals(2, allSessions.size());
        UUID familyId = allSessions.getFirst().getTokenFamilyId();
        List<AuthSessionEntity> family = sessionRepository.findByTokenFamilyIdOrderByCreatedAtAsc(familyId);
        assertEquals(2, family.size());
        assertEquals(1, family.stream().filter(session -> session.getRevokedAt() == null).count());
    }

    private boolean rotate(String token, CountDownLatch ready, CountDownLatch start) throws InterruptedException {
        ready.countDown();
        start.await();
        try {
            refreshAuthSessionUseCase.refresh(token, METADATA);
            return true;
        } catch (InvalidAuthSessionException exception) {
            return false;
        }
    }
}
