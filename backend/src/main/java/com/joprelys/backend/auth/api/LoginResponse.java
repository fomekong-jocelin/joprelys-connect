package com.joprelys.backend.auth.api;

import java.time.Instant;

public record LoginResponse(
		String accessToken,
		String tokenType,
		Instant expiresAt,
		String email,
		String name,
		String role,
		Boolean requiresOtp) {

	public LoginResponse(String accessToken, String tokenType, Instant expiresAt, String email, String name, String role) {
		this(accessToken, tokenType, expiresAt, email, name, role, false);
	}
}
