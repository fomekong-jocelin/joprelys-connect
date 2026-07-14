package com.joprelys.backend.auth.session.application;

import com.joprelys.backend.auth.session.infrastructure.persistence.AuthSessionRepository;
import java.time.Clock;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthSessionCleanupService {

    private final AuthSessionRepository sessionRepository;
    private final AuthSessionExpiryPolicy expiryPolicy;
    private final Clock clock;

    public AuthSessionCleanupService(
            AuthSessionRepository sessionRepository,
            AuthSessionExpiryPolicy expiryPolicy,
            Clock clock) {
        this.sessionRepository = sessionRepository;
        this.expiryPolicy = expiryPolicy;
        this.clock = clock;
    }

    @Scheduled(cron = "${joprelys.security.sessions.cleanup-cron:0 15 * * * *}")
    @Transactional
    public long purgeExpiredSessions() {
        return sessionRepository.deleteByAbsoluteExpiresAtBefore(
                expiryPolicy.retentionCutoffFrom(clock.instant()));
    }
}
