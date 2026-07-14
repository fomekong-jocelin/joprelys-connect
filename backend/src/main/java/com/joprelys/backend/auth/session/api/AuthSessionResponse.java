package com.joprelys.backend.auth.session.api;

import java.time.Instant;
import java.util.UUID;

public record AuthSessionResponse(
        UUID id,
        boolean current,
        String clientType,
        String deviceLabel,
        String networkHint,
        Instant createdAt,
        Instant lastUsedAt,
        Instant expiresAt,
        String state,
        String revocationReason) {
}
