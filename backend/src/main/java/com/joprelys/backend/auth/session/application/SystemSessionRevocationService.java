package com.joprelys.backend.auth.session.application;

import com.joprelys.backend.auth.session.domain.AuthSessionAuditEvent;
import com.joprelys.backend.auth.session.domain.AuthSessionAuditEventType;
import com.joprelys.backend.auth.session.domain.AuthSessionRevocationReason;
import com.joprelys.backend.auth.session.domain.AuthSessionRevocationSource;
import com.joprelys.backend.auth.session.infrastructure.persistence.AuthSessionEntity;
import com.joprelys.backend.auth.session.infrastructure.persistence.AuthSessionRepository;
import java.time.Clock;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class SystemSessionRevocationService implements RevokeAllUserSessionsUseCase {

    private final AuthSessionRepository sessionRepository;
    private final AuthSessionAuditPort auditPort;
    private final Clock clock;

    public SystemSessionRevocationService(
            AuthSessionRepository sessionRepository,
            AuthSessionAuditPort auditPort,
            Clock clock) {
        this.sessionRepository = sessionRepository;
        this.auditPort = auditPort;
        this.clock = clock;
    }

    @Override
    @Transactional
    public int revokeAllForUser(UUID userId, AuthSessionRevocationReason reason) {
        Instant now = clock.instant();
        List<AuthSessionEntity> revoked = new ArrayList<>();
        for (AuthSessionEntity session : sessionRepository.findByUserIdForUpdate(userId)) {
            if (session.revoke(reason, AuthSessionRevocationSource.SYSTEM, null, now)) {
                revoked.add(session);
            }
        }
        if (revoked.isEmpty()) {
            return 0;
        }
        sessionRepository.saveAll(revoked);
        revoked.forEach(session -> auditPort.append(new AuthSessionAuditEvent(
                session.getOrganizationId(),
                null,
                userId,
                session.getId(),
                session.getTokenFamilyId(),
                AuthSessionAuditEventType.SESSION_REVOKED,
                reason.name(),
                now)));
        return revoked.size();
    }
}
