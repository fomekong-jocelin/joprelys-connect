package com.joprelys.backend.auth.security;

import com.joprelys.backend.auth.session.infrastructure.persistence.RevokedAccessTokenEntity;
import com.joprelys.backend.auth.session.infrastructure.persistence.RevokedAccessTokenRepository;
import java.time.Clock;
import java.time.Instant;
import java.util.UUID;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class JwtRevocationService {

    private final RevokedAccessTokenRepository repository;
    private final Clock clock;

    public JwtRevocationService(RevokedAccessTokenRepository repository, Clock clock) {
        this.repository = repository;
        this.clock = clock;
    }

    @Transactional
    public boolean revoke(String tokenId, Instant expiresAt) {
        return revoke(tokenId, null, null, expiresAt, "LOGOUT", null);
    }

    @Transactional
    public boolean revoke(JwtClaims claims, UUID actorUserId, String reason) {
        return revoke(
                claims.tokenId(),
                parseUuid(claims.subject()),
                parseUuid(claims.organizationId()),
                claims.expiresAt(),
                reason,
                actorUserId);
    }

    @Transactional
    public boolean revoke(
            String tokenId,
            UUID userId,
            UUID organizationId,
            Instant expiresAt,
            String reason,
            UUID actorUserId) {
        Instant now = clock.instant();
        if (tokenId == null || tokenId.isBlank() || expiresAt == null || !expiresAt.isAfter(now)) {
            return false;
        }
        if (repository.existsById(tokenId)) {
            return false;
        }
        try {
            repository.save(new RevokedAccessTokenEntity(
                    tokenId,
                    userId,
                    organizationId,
                    now,
                    expiresAt,
                    reason,
                    actorUserId));
            return true;
        } catch (DataIntegrityViolationException duplicate) {
            return false;
        }
    }

    @Transactional(readOnly = true)
    public boolean isRevoked(String tokenId) {
        return repository.existsByTokenIdAndExpiresAtAfter(tokenId, clock.instant());
    }

    @Scheduled(cron = "${joprelys.security.sessions.cleanup-cron:0 15 * * * *}")
    @Transactional
    public void purgeExpiredTokens() {
        repository.deleteByExpiresAtBefore(clock.instant());
    }

    private static UUID parseUuid(String value) {
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
