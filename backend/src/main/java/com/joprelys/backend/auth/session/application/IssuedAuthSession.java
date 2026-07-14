package com.joprelys.backend.auth.session.application;

import java.time.Instant;
import java.util.UUID;

public record IssuedAuthSession(
        String accessToken,
        Instant accessTokenExpiresAt,
        UUID sessionId,
        Instant sessionExpiresAt,
        String refreshToken) {
}
