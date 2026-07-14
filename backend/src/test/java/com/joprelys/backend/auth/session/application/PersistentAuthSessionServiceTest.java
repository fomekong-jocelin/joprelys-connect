package com.joprelys.backend.auth.session.application;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.joprelys.backend.auth.infrastructure.persistence.UserAccountEntity;
import com.joprelys.backend.auth.security.JwtService;
import com.joprelys.backend.auth.session.config.AuthSessionProperties;
import com.joprelys.backend.auth.session.domain.AuthSessionAuditEvent;
import com.joprelys.backend.auth.session.domain.AuthSessionAuditEventType;
import com.joprelys.backend.auth.session.domain.AuthSessionRevocationReason;
import com.joprelys.backend.auth.session.infrastructure.persistence.AuthSessionEntity;
import com.joprelys.backend.auth.session.infrastructure.persistence.AuthSessionRepository;
import com.joprelys.backend.clinic.infrastructure.persistence.OrganizationEntity;
import com.joprelys.backend.clinic.infrastructure.persistence.OrganizationRepository;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class PersistentAuthSessionServiceTest {

    private static final Instant NOW = Instant.parse("2026-07-14T08:00:00Z");
    private static final Clock CLOCK = Clock.fixed(NOW, ZoneOffset.UTC);
    private static final SessionClientMetadata METADATA = new SessionClientMetadata(
            "WEB", "JUnit", "127.0.0.0/24");

    @Mock
    private AuthSessionRepository sessionRepository;
    @Mock
    private RefreshTokenGenerator tokenGenerator;
    @Mock
    private RefreshTokenHasher tokenHasher;
    @Mock
    private AuthSessionAuditPort auditPort;
    @Mock
    private JwtService jwtService;
    @Mock
    private OrganizationRepository organizationRepository;

    private PersistentAuthSessionService service;

    @BeforeEach
    void setUp() {
        AuthSessionProperties properties = new AuthSessionProperties(
                24,
                30,
                24,
                "joprelys_refresh",
                false,
                "Lax",
                "/api/auth",
                "0 0 0 1 1 *");
        service = new PersistentAuthSessionService(
                sessionRepository,
                tokenGenerator,
                tokenHasher,
                new AuthSessionExpiryPolicy(properties),
                auditPort,
                jwtService,
                organizationRepository,
                CLOCK);
    }

    @Test
    void shouldIssueHashedPersistentSessionAndAuditCreation() {
        UserAccountEntity user = user();
        when(tokenGenerator.generate()).thenReturn("raw-refresh-token");
        when(tokenHasher.hash("raw-refresh-token")).thenReturn("a".repeat(64));
        when(jwtService.createToken(eq(user), any(UUID.class))).thenReturn(accessToken());

        IssuedAuthSession result = service.issue(user, METADATA);

        ArgumentCaptor<AuthSessionEntity> captor = ArgumentCaptor.forClass(AuthSessionEntity.class);
        verify(sessionRepository).save(captor.capture());
        AuthSessionEntity persisted = captor.getValue();
        assertEquals("a".repeat(64), persisted.getRefreshTokenHash());
        assertNotEquals("raw-refresh-token", persisted.getRefreshTokenHash());
        assertEquals(persisted.getId(), result.sessionId());
        assertEquals("raw-refresh-token", result.refreshToken());
        assertEquals(NOW.plusSeconds(1_800), result.sessionExpiresAt());
        verify(auditPort).append(any(AuthSessionAuditEvent.class));
    }

    @Test
    void shouldRotateOncePreserveFamilyAndAuditRotation() {
        UserAccountEntity user = user();
        UUID familyId = UUID.randomUUID();
        AuthSessionEntity current = AuthSessionEntity.create(
                user,
                familyId,
                "b".repeat(64),
                METADATA,
                NOW.minusSeconds(300),
                NOW.plusSeconds(3_600),
                NOW.plusSeconds(600));
        when(tokenHasher.hash("old-token")).thenReturn("b".repeat(64));
        when(sessionRepository.findByRefreshTokenHashForUpdate("b".repeat(64)))
                .thenReturn(Optional.of(current));
        when(tokenGenerator.generate()).thenReturn("new-token");
        when(tokenHasher.hash("new-token")).thenReturn("c".repeat(64));
        when(jwtService.createToken(eq(user), any(UUID.class))).thenReturn(accessToken());

        IssuedAuthSession result = service.refresh("old-token", METADATA);

        ArgumentCaptor<AuthSessionEntity> replacementCaptor = ArgumentCaptor.forClass(AuthSessionEntity.class);
        verify(sessionRepository).saveAndFlush(replacementCaptor.capture());
        AuthSessionEntity replacement = replacementCaptor.getValue();
        assertEquals(familyId, replacement.getTokenFamilyId());
        assertEquals(current.getAbsoluteExpiresAt(), replacement.getAbsoluteExpiresAt());
        assertEquals(AuthSessionRevocationReason.ROTATED.name(), current.getRevocationReason());
        assertEquals(replacement.getId(), current.getReplacedBySessionId());
        assertEquals(replacement.getId(), result.sessionId());
        assertEquals("new-token", result.refreshToken());
        verify(auditPort).append(any(AuthSessionAuditEvent.class));
    }

    @Test
    void shouldRevokeActiveFamilyAndAuditWhenRotatedTokenIsReused() {
        UserAccountEntity user = user();
        UUID familyId = UUID.randomUUID();
        AuthSessionEntity consumed = AuthSessionEntity.create(
                user,
                familyId,
                "b".repeat(64),
                METADATA,
                NOW.minusSeconds(300),
                NOW.plusSeconds(3_600),
                NOW.plusSeconds(600));
        AuthSessionEntity active = AuthSessionEntity.create(
                user,
                familyId,
                "c".repeat(64),
                METADATA,
                NOW.minusSeconds(1),
                NOW.plusSeconds(3_600),
                NOW.plusSeconds(600));
        consumed.replaceWith(active, NOW.minusSeconds(1));
        when(tokenHasher.hash("old-token")).thenReturn("b".repeat(64));
        when(sessionRepository.findByRefreshTokenHashForUpdate("b".repeat(64)))
                .thenReturn(Optional.of(consumed));
        when(sessionRepository.findByTokenFamilyIdForUpdate(familyId))
                .thenReturn(List.of(consumed, active));
        when(auditPort.exists(AuthSessionAuditEventType.REFRESH_REPLAY_DETECTED, familyId))
                .thenReturn(false);

        assertThrows(
                InvalidAuthSessionException.class,
                () -> service.refresh("old-token", METADATA));

        assertEquals(AuthSessionRevocationReason.REPLAY_DETECTED.name(), active.getRevocationReason());
        verify(sessionRepository).saveAll(List.of(consumed, active));
        verify(auditPort).append(any(AuthSessionAuditEvent.class));
    }

    @Test
    void shouldRejectRefreshWhenOrganizationIsInactive() {
        UserAccountEntity user = user();
        UUID organizationId = UUID.randomUUID();
        user.setOrganizationId(organizationId);
        OrganizationEntity organization = new OrganizationEntity(
                "Clinic", "clinic@example.com", "123", "Street", "Douala");
        organization.setStatus("INACTIVE");
        AuthSessionEntity current = AuthSessionEntity.create(
                user,
                UUID.randomUUID(),
                "b".repeat(64),
                METADATA,
                NOW.minusSeconds(300),
                NOW.plusSeconds(3_600),
                NOW.plusSeconds(600));
        when(tokenHasher.hash("old-token")).thenReturn("b".repeat(64));
        when(sessionRepository.findByRefreshTokenHashForUpdate("b".repeat(64)))
                .thenReturn(Optional.of(current));
        when(organizationRepository.findById(organizationId)).thenReturn(Optional.of(organization));

        assertThrows(
                InvalidAuthSessionException.class,
                () -> service.refresh("old-token", METADATA));
    }

    private static UserAccountEntity user() {
        return new UserAccountEntity(
                "agent@example.com",
                "Agent Accueil",
                "AGENT_ACCUEIL",
                "hash");
    }

    private static JwtService.CreatedToken accessToken() {
        return new JwtService.CreatedToken(
                "jwt-token",
                "jti",
                NOW.plusSeconds(900));
    }
}
