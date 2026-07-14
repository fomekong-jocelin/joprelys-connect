package com.joprelys.backend.auth.security;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.joprelys.backend.auth.session.infrastructure.persistence.RevokedAccessTokenRepository;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class JwtRevocationServiceTest {

    private static final Instant NOW = Instant.parse("2026-07-14T10:00:00Z");
    private static final Clock CLOCK = Clock.fixed(NOW, ZoneOffset.UTC);

    @Mock RevokedAccessTokenRepository repository;

    private JwtRevocationService service;

    @BeforeEach
    void setUp() {
        service = new JwtRevocationService(repository, CLOCK);
    }

    @Test
    void shouldPersistLegacyJtiWithoutStoringRawToken() {
        JwtClaims claims = claims(NOW.plusSeconds(900));
        when(repository.existsById(claims.tokenId())).thenReturn(false);

        assertTrue(service.revoke(claims, UUID.fromString(claims.subject()), "LOGOUT"));

        verify(repository).save(any());
    }

    @Test
    void shouldBeIdempotentWhenJtiAlreadyExists() {
        JwtClaims claims = claims(NOW.plusSeconds(900));
        when(repository.existsById(claims.tokenId())).thenReturn(true);

        assertFalse(service.revoke(claims, UUID.fromString(claims.subject()), "LOGOUT"));

        verify(repository, never()).save(any());
    }

    @Test
    void shouldIgnoreAlreadyExpiredJtiAndPurgeExpiredRows() {
        JwtClaims claims = claims(NOW.minusSeconds(1));

        assertFalse(service.revoke(claims, UUID.fromString(claims.subject()), "LOGOUT"));
        service.purgeExpiredTokens();

        verify(repository, never()).save(any());
        verify(repository).deleteByExpiresAtBefore(NOW);
    }

    @Test
    void shouldReadRevocationFromPersistentRepository() {
        String tokenId = UUID.randomUUID().toString();
        when(repository.existsByTokenIdAndExpiresAtAfter(tokenId, NOW)).thenReturn(true);

        assertTrue(service.isRevoked(tokenId));
    }

    private static JwtClaims claims(Instant expiresAt) {
        UUID userId = UUID.randomUUID();
        UUID organizationId = UUID.randomUUID();
        return new JwtClaims(
                userId.toString(),
                "legacy@example.com",
                "Legacy",
                "AGENT_ACCUEIL",
                organizationId.toString(),
                UUID.randomUUID().toString(),
                expiresAt);
    }
}
