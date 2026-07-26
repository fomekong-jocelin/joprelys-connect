package com.joprelys.backend.auth.session.application;

import com.joprelys.backend.auth.security.JwtClaims;
import com.joprelys.backend.auth.security.JwtRevocationService;
import com.joprelys.backend.auth.session.domain.AuthSessionRevocationReason;
import com.joprelys.backend.auth.session.infrastructure.persistence.AuthSessionEntity;
import com.joprelys.backend.auth.session.infrastructure.persistence.AuthSessionRepository;
import java.time.Clock;
import java.time.Instant;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PersistentAccessTokenSessionValidator implements AccessTokenSessionValidator {

    private static final int MAX_ROTATION_CHAIN_DEPTH = 64;

    private final AuthSessionRepository sessionRepository;
    private final JwtRevocationService jwtRevocationService;
    private final Clock clock;

    public PersistentAccessTokenSessionValidator(
            AuthSessionRepository sessionRepository,
            JwtRevocationService jwtRevocationService,
            Clock clock) {
        this.sessionRepository = sessionRepository;
        this.jwtRevocationService = jwtRevocationService;
        this.clock = clock;
    }

    @Override
    @Transactional(readOnly = true)
    public boolean isValid(JwtClaims claims) {
        Optional<UUID> sessionId = parseUuid(claims.sessionId());
        if (sessionId.isEmpty()) {
            return !jwtRevocationService.isRevoked(claims.tokenId());
        }

        Optional<UUID> userId = parseUuid(claims.subject());
        if (userId.isEmpty()) {
            return false;
        }

        AuthSessionEntity session = sessionRepository.findByIdWithUser(sessionId.get()).orElse(null);
        if (session == null) {
            return false;
        }
        return isValidThroughRotationChain(session, userId.get(), claims.organizationId(), clock.instant());
    }

    private boolean isValidThroughRotationChain(
            AuthSessionEntity initial,
            UUID userId,
            String organizationClaim,
            Instant now) {
        AuthSessionEntity current = initial;
        for (int depth = 0; depth < MAX_ROTATION_CHAIN_DEPTH; depth++) {
            if (!belongsToClaims(current, userId, organizationClaim)) {
                return false;
            }
            if (current.isActiveAt(now)) {
                return true;
            }
            if (!AuthSessionRevocationReason.ROTATED.name().equals(current.getRevocationReason())
                    || current.getReplacedBySessionId() == null) {
                return false;
            }
            current = sessionRepository.findByIdWithUser(current.getReplacedBySessionId()).orElse(null);
            if (current == null) {
                return false;
            }
        }
        return false;
    }

    private static boolean belongsToClaims(
            AuthSessionEntity session,
            UUID userId,
            String organizationClaim) {
        UUID claimedOrganizationId = parseUuid(organizationClaim).orElse(null);
        return session.getUser().getId().equals(userId)
                && Objects.equals(session.getOrganizationId(), claimedOrganizationId);
    }

    private static Optional<UUID> parseUuid(String value) {
        if (value == null || value.isBlank()) {
            return Optional.empty();
        }
        try {
            return Optional.of(UUID.fromString(value));
        } catch (IllegalArgumentException exception) {
            return Optional.empty();
        }
    }
}
