package com.joprelys.backend.auth.session.application;

import java.time.Instant;
import java.util.UUID;

public record AuthSessionView(
        UUID id,
        boolean current,
        String clientType,
        String userAgent,
        String networkHint,
        Instant createdAt,
        Instant lastUsedAt,
        Instant expiresAt,
        String state,
        String revocationReason) {
}
