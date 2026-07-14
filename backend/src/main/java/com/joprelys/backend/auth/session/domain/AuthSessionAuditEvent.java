package com.joprelys.backend.auth.session.domain;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

public record AuthSessionAuditEvent(
        UUID organizationId,
        UUID actorUserId,
        UUID targetUserId,
        UUID sessionId,
        UUID tokenFamilyId,
        AuthSessionAuditEventType eventType,
        String reason,
        Instant occurredAt) {

    public AuthSessionAuditEvent {
        Objects.requireNonNull(targetUserId);
        Objects.requireNonNull(eventType);
        Objects.requireNonNull(occurredAt);
    }
}
