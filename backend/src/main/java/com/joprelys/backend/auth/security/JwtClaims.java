package com.joprelys.backend.auth.security;

import java.time.Instant;

public record JwtClaims(
        String subject,
        String email,
        String displayName,
        String role,
        String organizationId,
        String tokenId,
        String sessionId,
        Instant expiresAt) {

    public JwtClaims(
            String subject,
            String email,
            String displayName,
            String role,
            String organizationId,
            String tokenId,
            Instant expiresAt) {
        this(subject, email, displayName, role, organizationId, tokenId, "", expiresAt);
    }
}
