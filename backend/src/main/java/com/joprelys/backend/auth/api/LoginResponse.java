package com.joprelys.backend.auth.api;

import java.time.Instant;
import java.util.UUID;

public record LoginResponse(
        String accessToken,
        String tokenType,
        Instant expiresAt,
        Instant sessionExpiresAt,
        UUID sessionId,
        String email,
        String name,
        String role,
        Boolean requiresOtp,
        String otpCode) {

    public LoginResponse(
            String accessToken,
            String tokenType,
            Instant expiresAt,
            String email,
            String name,
            String role,
            Boolean requiresOtp,
            String otpCode) {
        this(accessToken, tokenType, expiresAt, null, null, email, name, role, requiresOtp, otpCode);
    }

    public LoginResponse(
            String accessToken,
            String tokenType,
            Instant expiresAt,
            String email,
            String name,
            String role,
            Boolean requiresOtp) {
        this(accessToken, tokenType, expiresAt, null, null, email, name, role, requiresOtp, null);
    }

    public LoginResponse(
            String accessToken,
            String tokenType,
            Instant expiresAt,
            String email,
            String name,
            String role) {
        this(accessToken, tokenType, expiresAt, null, null, email, name, role, false, null);
    }
}
