package com.joprelys.backend.auth.session.api;

import com.joprelys.backend.auth.security.InvalidTokenException;
import com.joprelys.backend.auth.security.JwtClaims;
import com.joprelys.backend.auth.session.application.SessionActor;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;

@Component
public class SessionActorFactory {

    public SessionActor from(Authentication authentication) {
        JwtClaims claims = claims(authentication);
        UUID userId = requiredUuid(claims.subject());
        Set<String> authorities = authentication.getAuthorities().stream()
                .map(authority -> authority.getAuthority())
                .collect(Collectors.toUnmodifiableSet());
        return new SessionActor(
                userId,
                optionalUuid(claims.organizationId()),
                optionalUuid(claims.sessionId()),
                authorities);
    }

    public JwtClaims claims(Authentication authentication) {
        if (authentication == null || !(authentication.getDetails() instanceof JwtClaims claims)) {
            throw new InvalidTokenException("Missing authenticated JWT claims");
        }
        return claims;
    }

    private static UUID requiredUuid(String value) {
        UUID parsed = optionalUuid(value);
        if (parsed == null) {
            throw new InvalidTokenException("Invalid staff subject");
        }
        return parsed;
    }

    private static UUID optionalUuid(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            return UUID.fromString(value);
        } catch (IllegalArgumentException exception) {
            return null;
        }
    }
}
