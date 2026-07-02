package com.joprelys.backend.auth.security;

import java.time.Instant;

public record JwtClaims(
		String subject,
		String email,
		String displayName,
		String role,
		String organizationId,
		String tokenId,
		Instant expiresAt) {
}
