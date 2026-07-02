package com.joprelys.backend.auth.api;

import java.time.Instant;

public record LoginResponse(
		String accessToken,
		String tokenType,
		Instant expiresAt,
		String email,
		String name,
		String role) {
}
