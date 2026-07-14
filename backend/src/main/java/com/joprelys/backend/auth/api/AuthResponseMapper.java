package com.joprelys.backend.auth.api;

import com.joprelys.backend.auth.application.AuthenticationOutcome;
import com.joprelys.backend.auth.session.application.IssuedAuthSession;
import org.springframework.stereotype.Component;

@Component
public class AuthResponseMapper {

    public LoginResponse toResponse(AuthenticationOutcome outcome) {
        if (outcome.requiresOtp()) {
            return new LoginResponse(
                    null,
                    null,
                    null,
                    null,
                    null,
                    outcome.email(),
                    outcome.displayName(),
                    outcome.role(),
                    true,
                    outcome.otpCode());
        }
        return toResponse(outcome.session());
    }

    public LoginResponse toResponse(IssuedAuthSession session) {
        return new LoginResponse(
                session.accessToken(),
                "Bearer",
                session.accessTokenExpiresAt(),
                session.sessionExpiresAt(),
                session.sessionId(),
                session.email(),
                session.displayName(),
                session.role(),
                false,
                null);
    }
}
