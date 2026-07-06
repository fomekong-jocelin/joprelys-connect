package com.joprelys.backend.auth.api;

import java.time.Instant;

public record LoginResponse(
		String accessToken,
		String tokenType,
		Instant expiresAt,
		String email,
		String name,
		String role,
		Boolean requiresOtp,
		String otpCode) {

	public LoginResponse(String accessToken, String tokenType, Instant expiresAt, String email, String name, String role, Boolean requiresOtp) {
		this(accessToken, tokenType, expiresAt, email, name, role, requiresOtp, null);
	}

	public LoginResponse(String accessToken, String tokenType, Instant expiresAt, String email, String name, String role) {
		this(accessToken, tokenType, expiresAt, email, name, role, false, null);
	}
}
