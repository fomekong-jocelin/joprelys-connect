package com.joprelys.backend.auth.session.application;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.joprelys.backend.auth.infrastructure.persistence.UserAccountEntity;
import com.joprelys.backend.auth.infrastructure.persistence.UserAccountRepository;
import com.joprelys.backend.auth.session.domain.AuthSessionAuditEventType;
import com.joprelys.backend.auth.session.domain.AuthSessionRevocationReason;
import com.joprelys.backend.auth.session.infrastructure.persistence.AuthSessionAuditEventRepository;
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
    @Autowired AuthSessionAuditEventRepository auditRepository;
    @Autowired UserAccountRepository userAccountRepository;
    @Autowired OrganizationRepository organizationRepository;

    private ExecutorService executor;

    @BeforeEach
    void setUp() {
        auditRepository.deleteAll();
        sessionRepository.deleteAll();
        executor = Executors.newFixedThreadPool(2);
    }

    @AfterEach
    void tearDown() {
        executor.shutdownNow();
    }

    @Test
    void shouldAllowOneRefreshAndTreatTheOtherAsBenignConcurrency() throws Exception {
        String suffix = UUID.randomUUID().toString().substring(0, 8);
        OrganizationEntity organization = organizationRepository.save(new OrganizationEntity(
                "Clinique Concurrence " + suffix,
                "concurrency-" + suffix + "@joprelys.local",
                "+237600000002",
                "Rue A",
                "Douala"));
        UserAccountEntity user = new UserAccountEntity(
                "concurrency.user-" + suffix + "@joprelys.local",
                "Concurrency User",
                "AGENT_ACCUEIL",
                "hash");
        user.setOrganizationId(organization.getId());
        user = userAccountRepository.save(user);
        IssuedAuthSession issued = issueAuthSessionUseCase.issue(user, METADATA);

        CountDownLatch ready = new CountDownLatch(2);
        CountDownLatch start = new CountDownLatch(1);
        Future<RefreshOutcome> first = executor.submit(() -> rotate(issued.refreshToken(), ready, start));
        Future<RefreshOutcome> second = executor.submit(() -> rotate(issued.refreshToken(), ready, start));
        ready.await();
        start.countDown();

        List<RefreshOutcome> outcomes = List.of(first.get(), second.get());
        assertEquals(1, outcomes.stream().filter(RefreshOutcome.SUCCESS::equals).count());
        assertEquals(1, outcomes.stream().filter(RefreshOutcome.CONCURRENT::equals).count());

        List<AuthSessionEntity> allSessions = sessionRepository.findAll();
        assertEquals(2, allSessions.size());
        UUID familyId = allSessions.getFirst().getTokenFamilyId();
        List<AuthSessionEntity> family = sessionRepository.findByTokenFamilyIdOrderByCreatedAtAsc(familyId);
        assertEquals(1, family.stream().filter(session -> session.getRevokedAt() == null).count());
        assertTrue(family.stream().anyMatch(session ->
                AuthSessionRevocationReason.ROTATED.name().equals(session.getRevocationReason())));
        assertFalse(family.stream().anyMatch(session ->
                AuthSessionRevocationReason.REPLAY_DETECTED.name().equals(session.getRevocationReason())));
        assertFalse(auditRepository.existsByEventTypeAndTokenFamilyId(
                AuthSessionAuditEventType.REFRESH_REPLAY_DETECTED.name(), familyId));
    }

    private RefreshOutcome rotate(String token, CountDownLatch ready, CountDownLatch start) throws InterruptedException {
        ready.countDown();
        start.await();
        try {
            refreshAuthSessionUseCase.refresh(token, METADATA);
            return RefreshOutcome.SUCCESS;
        } catch (ConcurrentAuthRefreshException exception) {
            return RefreshOutcome.CONCURRENT;
        } catch (InvalidAuthSessionException exception) {
            return RefreshOutcome.INVALID;
        }
    }

    private enum RefreshOutcome {
        SUCCESS,
        CONCURRENT,
        INVALID
    }
}
