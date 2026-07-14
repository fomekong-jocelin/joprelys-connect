package com.joprelys.backend.auth.session.application;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.joprelys.backend.auth.infrastructure.persistence.UserAccountEntity;
import com.joprelys.backend.auth.infrastructure.persistence.UserAccountRepository;
import com.joprelys.backend.auth.security.JwtClaims;
import com.joprelys.backend.auth.security.JwtRevocationService;
import com.joprelys.backend.auth.session.domain.AuthSessionRevocationReason;
import com.joprelys.backend.auth.session.infrastructure.persistence.AuthSessionEntity;
import com.joprelys.backend.auth.session.infrastructure.persistence.AuthSessionRepository;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class AuthSessionManagementServiceTest {

    private static final Instant NOW = Instant.parse("2026-07-14T09:00:00Z");
    private static final Clock CLOCK = Clock.fixed(NOW, ZoneOffset.UTC);
    private static final SessionClientMetadata METADATA = new SessionClientMetadata(
            "WEB", "Mozilla/5.0 Chrome/150 Windows", "192.168.1.0/24");

    @Mock AuthSessionRepository sessionRepository;
    @Mock UserAccountRepository userRepository;
    @Mock AuthSessionAuditPort auditPort;
    @Mock JwtRevocationService jwtRevocationService;

    private AuthSessionManagementService service;

    @BeforeEach
    void setUp() {
        service = new AuthSessionManagementService(
                sessionRepository,
                userRepository,
                auditPort,
                jwtRevocationService,
                CLOCK);
    }

    @Test
    void shouldListOwnSessionsAndMarkCurrentGeneration() {
        UUID organizationId = UUID.randomUUID();
        UserAccountEntity user = user("owner@example.com", organizationId);
        AuthSessionEntity session = session(user, UUID.randomUUID());
        SessionActor actor = actor(user, organizationId, session.getId(), Set.of());
        when(sessionRepository.findByUserIdOrderByCreatedAtDesc(user.getId()))
                .thenReturn(List.of(session));

        List<AuthSessionView> result = service.listOwn(actor);

        assertEquals(1, result.size());
        assertTrue(result.getFirst().current());
        assertEquals("ACTIVE", result.getFirst().state());
        assertEquals("192.168.1.0/24", result.getFirst().networkHint());
    }

    @Test
    void shouldRevokeOwnSessionIdempotently() {
        UUID organizationId = UUID.randomUUID();
        UserAccountEntity user = user("owner@example.com", organizationId);
        AuthSessionEntity session = session(user, UUID.randomUUID());
        SessionActor actor = actor(user, organizationId, session.getId(), Set.of());
        when(sessionRepository.findByIdForUpdate(session.getId()))
                .thenReturn(Optional.of(session));

        service.revoke(actor, session.getId());
        service.revoke(actor, session.getId());

        assertEquals(AuthSessionRevocationReason.LOGOUT.name(), session.getRevocationReason());
        assertEquals("SELF", session.getRevocationSource());
        assertEquals(user.getId(), session.getRevokedByUserId());
        verify(sessionRepository).save(session);
        verify(auditPort).append(any());
    }

    @Test
    void shouldAllowAuthorizedAdministratorWithinSameTenant() {
        UUID organizationId = UUID.randomUUID();
        UserAccountEntity admin = user("admin@example.com", organizationId);
        UserAccountEntity target = user("target@example.com", organizationId);
        AuthSessionEntity session = session(target, UUID.randomUUID());
        SessionActor actor = actor(
                admin,
                organizationId,
                UUID.randomUUID(),
                Set.of("AUTH_SESSION_MANAGE"));
        when(sessionRepository.findByIdForUpdate(session.getId()))
                .thenReturn(Optional.of(session));

        service.revoke(actor, session.getId());

        assertEquals(AuthSessionRevocationReason.ADMIN_REVOKED.name(), session.getRevocationReason());
        assertEquals("ADMIN", session.getRevocationSource());
        assertEquals(admin.getId(), session.getRevokedByUserId());
    }

    @Test
    void shouldHideCrossTenantSessionEvenFromAuthorizedAdministrator() {
        UUID adminOrganizationId = UUID.randomUUID();
        UserAccountEntity admin = user("admin@example.com", adminOrganizationId);
        UserAccountEntity target = user("target@example.com", UUID.randomUUID());
        AuthSessionEntity session = session(target, UUID.randomUUID());
        SessionActor actor = actor(
                admin,
                adminOrganizationId,
                UUID.randomUUID(),
                Set.of("AUTH_SESSION_MANAGE"));
        when(sessionRepository.findByIdForUpdate(session.getId()))
                .thenReturn(Optional.of(session));

        assertThrows(
                AuthSessionNotFoundException.class,
                () -> service.revoke(actor, session.getId()));

        verify(sessionRepository, never()).save(any());
        verify(auditPort, never()).append(any());
    }

    @Test
    void shouldRejectCrossUserRevocationWithoutPermission() {
        UUID organizationId = UUID.randomUUID();
        UserAccountEntity actorUser = user("actor@example.com", organizationId);
        UserAccountEntity target = user("target@example.com", organizationId);
        AuthSessionEntity session = session(target, UUID.randomUUID());
        SessionActor actor = actor(actorUser, organizationId, UUID.randomUUID(), Set.of());
        when(sessionRepository.findByIdForUpdate(session.getId()))
                .thenReturn(Optional.of(session));

        assertThrows(
                AuthSessionAccessDeniedException.class,
                () -> service.revoke(actor, session.getId()));
    }

    @Test
    void shouldRevokeAllActiveSessionsAndAuditOnce() {
        UUID organizationId = UUID.randomUUID();
        UserAccountEntity user = user("owner@example.com", organizationId);
        AuthSessionEntity first = session(user, UUID.randomUUID());
        AuthSessionEntity second = session(user, UUID.randomUUID());
        SessionActor actor = actor(user, organizationId, first.getId(), Set.of());
        when(sessionRepository.findByUserIdForUpdate(user.getId()))
                .thenReturn(List.of(first, second));

        service.logoutAll(actor);

        assertEquals(AuthSessionRevocationReason.LOGOUT_ALL.name(), first.getRevocationReason());
        assertEquals(AuthSessionRevocationReason.LOGOUT_ALL.name(), second.getRevocationReason());
        verify(sessionRepository).saveAll(List.of(first, second));
        verify(auditPort).append(any());
    }

    @Test
    void shouldRevokeLegacyJtiDuringLogoutAllEvenWithoutPersistentSessions() {
        UUID organizationId = UUID.randomUUID();
        UserAccountEntity user = user("legacy-all@example.com", organizationId);
        String tokenId = UUID.randomUUID().toString();
        SessionActor actor = new SessionActor(
                user.getId(),
                organizationId,
                null,
                tokenId,
                NOW.plusSeconds(900),
                Set.of());
        when(sessionRepository.findByUserIdForUpdate(user.getId())).thenReturn(List.of());
        when(jwtRevocationService.revoke(
                tokenId,
                user.getId(),
                organizationId,
                NOW.plusSeconds(900),
                "LOGOUT_ALL",
                user.getId()))
                .thenReturn(true);

        service.logoutAll(actor);

        verify(jwtRevocationService).revoke(
                tokenId,
                user.getId(),
                organizationId,
                NOW.plusSeconds(900),
                "LOGOUT_ALL",
                user.getId());
        verify(auditPort, times(2)).append(any());
    }

    @Test
    void shouldPersistAndAuditLegacyJtiWhenSidIsMissing() {
        UUID userId = UUID.randomUUID();
        UUID organizationId = UUID.randomUUID();
        JwtClaims claims = new JwtClaims(
                userId.toString(),
                "legacy@example.com",
                "Legacy User",
                "AGENT_ACCUEIL",
                organizationId.toString(),
                UUID.randomUUID().toString(),
                NOW.plusSeconds(900));
        when(jwtRevocationService.revoke(claims, userId, "LOGOUT")).thenReturn(true);

        service.logout(claims);

        verify(jwtRevocationService).revoke(claims, userId, "LOGOUT");
        verify(auditPort).append(any());
    }

    private static UserAccountEntity user(String email, UUID organizationId) {
        UserAccountEntity user = new UserAccountEntity(email, email, "AGENT_ACCUEIL", "hash");
        user.setOrganizationId(organizationId);
        return user;
    }

    private static AuthSessionEntity session(UserAccountEntity user, UUID familyId) {
        return AuthSessionEntity.create(
                user,
                familyId,
                UUID.randomUUID().toString().replace("-", "").repeat(2),
                METADATA,
                NOW.minusSeconds(60),
                NOW.plusSeconds(3_600),
                NOW.plusSeconds(1_800));
    }

    private static SessionActor actor(
            UserAccountEntity user,
            UUID organizationId,
            UUID currentSessionId,
            Set<String> authorities) {
        return new SessionActor(
                user.getId(),
                organizationId,
                currentSessionId,
                UUID.randomUUID().toString(),
                NOW.plusSeconds(900),
                authorities);
    }
}
