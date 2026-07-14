package com.joprelys.backend.auth.session.application;

import com.joprelys.backend.auth.session.infrastructure.persistence.AuthSessionEntity;
import java.time.Instant;
import java.util.UUID;
import org.springframework.stereotype.Component;

@Component
public class AuthSessionViewMapper {

    public AuthSessionView toView(
            AuthSessionEntity session,
            UUID currentSessionId,
            Instant now) {
        return new AuthSessionView(
                session.getId(),
                session.getId().equals(currentSessionId),
                session.getClientType(),
                session.getUserAgent(),
                session.getNetworkPrefix(),
                session.getCreatedAt(),
                session.getLastUsedAt(),
                effectiveExpiry(session),
                state(session, now),
                session.getRevocationReason());
    }

    private static String state(AuthSessionEntity session, Instant now) {
        if (session.getRevokedAt() != null) {
            return "REVOKED";
        }
        return session.isActiveAt(now) ? "ACTIVE" : "EXPIRED";
    }

    private static Instant effectiveExpiry(AuthSessionEntity session) {
        return session.getIdleExpiresAt().isBefore(session.getAbsoluteExpiresAt())
                ? session.getIdleExpiresAt()
                : session.getAbsoluteExpiresAt();
    }
}
