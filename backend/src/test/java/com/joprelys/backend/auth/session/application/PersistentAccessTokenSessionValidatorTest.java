package com.joprelys.backend.auth.session.application;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

import com.joprelys.backend.auth.infrastructure.persistence.UserAccountEntity;
import com.joprelys.backend.auth.security.JwtClaims;
import com.joprelys.backend.auth.security.JwtRevocationService;
import com.joprelys.backend.auth.session.domain.AuthSessionRevocationReason;
import com.joprelys.backend.auth.session.infrastructure.persistence.AuthSessionEntity;
import com.joprelys.backend.auth.session.infrastructure.persistence.AuthSessionRepository;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class PersistentAccessTokenSessionValidatorTest {

    private static final Instant NOW = Instant.parse("2026-07-14T10:00:00Z");
    private static final Clock CLOCK = Clock.fixed(NOW, ZoneOffset.UTC);
    private static final SessionClientMetadata METADATA = new SessionClientMetadata(
            "WEB", "JUnit", "10.0.0.0/24");

    @Mock AuthSessionRepository sessionRepository;
    @Mock JwtRevocationService jwtRevocationService;

    private PersistentAccessTokenSessionValidator validator;

    @BeforeEach
    void setUp() {
        validator = new PersistentAccessTokenSessionValidator(
                sessionRepository,
                jwtRevocationService,
                CLOCK);
    }

    @Test
    void shouldAcceptLegacyTokenOnlyWhenJtiIsNotRevoked() {
        JwtClaims claims = legacyClaims();
        when(jwtRevocationService.isRevoked(claims.tokenId())).thenReturn(false, true);

        assertTrue(validator.isValid(claims));
        assertFalse(validator.isValid(claims));
    }

    @Test
    void shouldAcceptActiveSessionBoundTokenWithMatchingUserAndTenant() {
        UUID organizationId = UUID.randomUUID();
        UserAccountEntity user = user(organizationId);
        AuthSessionEntity session = session(user);
        JwtClaims claims = claims(user, organizationId, session.getId());
        when(sessionRepository.findByIdWithUser(session.getId())).thenReturn(Optional.of(session));

        assertTrue(validator.isValid(claims));
    }

    @Test
    void shouldRejectSessionBoundTokenAfterRevocation() {
        UUID organizationId = UUID.randomUUID();
        UserAccountEntity user = user(organizationId);
        AuthSessionEntity session = session(user);
        session.revoke(AuthSessionRevocationReason.LOGOUT, NOW.minusSeconds(1));
        JwtClaims claims = claims(user, organizationId, session.getId());
        when(sessionRepository.findByIdWithUser(session.getId())).thenReturn(Optional.of(session));

        assertFalse(validator.isValid(claims));
    }

    @Test
    void shouldRejectSessionWhenTenantClaimDoesNotMatch() {
        UUID organizationId = UUID.randomUUID();
        UserAccountEntity user = user(organizationId);
        AuthSessionEntity session = session(user);
        JwtClaims claims = claims(user, UUID.randomUUID(), session.getId());
        when(sessionRepository.findByIdWithUser(session.getId())).thenReturn(Optional.of(session));

        assertFalse(validator.isValid(claims));
    }

    private static JwtClaims legacyClaims() {
        return new JwtClaims(
                UUID.randomUUID().toString(),
                "legacy@example.com",
                "Legacy",
                "AGENT_ACCUEIL",
                UUID.randomUUID().toString(),
                UUID.randomUUID().toString(),
                NOW.plusSeconds(900));
    }

    private static JwtClaims claims(UserAccountEntity user, UUID organizationId, UUID sessionId) {
        return new JwtClaims(
                user.getId().toString(),
                user.getEmail(),
                user.getDisplayName(),
                user.getRole(),
                organizationId.toString(),
                UUID.randomUUID().toString(),
                sessionId.toString(),
                NOW.plusSeconds(900));
    }

    private static UserAccountEntity user(UUID organizationId) {
        UserAccountEntity user = new UserAccountEntity(
                "session@example.com", "Session User", "AGENT_ACCUEIL", "hash");
        user.setOrganizationId(organizationId);
        return user;
    }

    private static AuthSessionEntity session(UserAccountEntity user) {
        return AuthSessionEntity.create(
                user,
                UUID.randomUUID(),
                "a".repeat(64),
                METADATA,
                NOW.minusSeconds(60),
                NOW.plusSeconds(3_600),
                NOW.plusSeconds(1_800));
    }
}
