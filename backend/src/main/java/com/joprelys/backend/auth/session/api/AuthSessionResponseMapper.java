package com.joprelys.backend.auth.session.api;

import com.joprelys.backend.auth.session.application.AuthSessionView;
import java.util.List;
import org.springframework.stereotype.Component;

@Component
public class AuthSessionResponseMapper {

    public List<AuthSessionResponse> toResponses(List<AuthSessionView> sessions) {
        return sessions.stream().map(this::toResponse).toList();
    }

    private AuthSessionResponse toResponse(AuthSessionView session) {
        return new AuthSessionResponse(
                session.id(),
                session.current(),
                session.clientType(),
                deviceLabel(session.userAgent()),
                session.networkHint(),
                session.createdAt(),
                session.lastUsedAt(),
                session.expiresAt(),
                session.state(),
                session.revocationReason());
    }

    private static String deviceLabel(String userAgent) {
        if (userAgent == null || userAgent.isBlank()) {
            return "UNKNOWN_DEVICE";
        }
        String browser = userAgent.contains("Edg/") ? "Edge"
                : userAgent.contains("Chrome/") ? "Chrome"
                : userAgent.contains("Firefox/") ? "Firefox"
                : userAgent.contains("Safari/") ? "Safari"
                : "BROWSER_OTHER";
        String platform = userAgent.contains("Windows") ? "Windows"
                : userAgent.contains("Android") ? "Android"
                : userAgent.contains("iPhone") || userAgent.contains("iPad") ? "iOS"
                : userAgent.contains("Mac OS") ? "macOS"
                : userAgent.contains("Linux") ? "Linux"
                : "PLATFORM_UNKNOWN";
        return browser + " / " + platform;
    }
}
