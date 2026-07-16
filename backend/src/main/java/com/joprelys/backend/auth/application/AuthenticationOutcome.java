package com.joprelys.backend.auth.application;

import com.joprelys.backend.auth.session.application.IssuedAuthSession;

public record AuthenticationOutcome(
        IssuedAuthSession session,
        String email,
        String displayName,
        String role,
        boolean requiresOtp) {

    public static AuthenticationOutcome authenticated(IssuedAuthSession session) {
        return new AuthenticationOutcome(
                session,
                session.email(),
                session.displayName(),
                session.role(),
                false);
    }

    public static AuthenticationOutcome otpChallenge(
            String email,
            String displayName,
            String role) {
        return new AuthenticationOutcome(null, email, displayName, role, true);
    }

    public String refreshToken() {
        return session == null ? null : session.refreshToken();
    }
}
