package com.joprelys.backend.auth.session.application;

import com.joprelys.backend.auth.infrastructure.persistence.UserAccountEntity;
import com.joprelys.backend.auth.infrastructure.persistence.UserAccountRepository;
import com.joprelys.backend.auth.security.JwtClaims;
import com.joprelys.backend.auth.security.JwtRevocationService;
import com.joprelys.backend.auth.session.domain.AuthSessionAuditEvent;
import com.joprelys.backend.auth.session.domain.AuthSessionAuditEventType;
import com.joprelys.backend.auth.session.domain.AuthSessionRevocationReason;
import com.joprelys.backend.auth.session.domain.AuthSessionRevocationSource;
import com.joprelys.backend.auth.session.infrastructure.persistence.AuthSessionEntity;
import com.joprelys.backend.auth.session.infrastructure.persistence.AuthSessionRepository;
import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthSessionManagementService implements
        ListAuthSessionsUseCase,
        RevokeAuthSessionUseCase,
        LogoutAllSessionsUseCase,
        LogoutCurrentSessionUseCase {

    private final AuthSessionRepository sessionRepository;
    private final UserAccountRepository userRepository;
    private final AuthSessionAuditPort auditPort;
    private final JwtRevocationService jwtRevocationService;
    private final Clock clock;

    public AuthSessionManagementService(
            AuthSessionRepository sessionRepository,
            UserAccountRepository userRepository,
            AuthSessionAuditPort auditPort,
            JwtRevocationService jwtRevocationService,
            Clock clock) {
        this.sessionRepository = sessionRepository;
        this.userRepository = userRepository;
        this.auditPort = auditPort;
        this.jwtRevocationService = jwtRevocationService;
        this.clock = clock;
    }

    @Override
    @Transactional(readOnly = true)
    public List<AuthSessionView> listOwn(SessionActor actor) {
        return views(actor, actor.userId());
    }

    @Override
    @Transactional(readOnly = true)
    public List<AuthSessionView> listUser(SessionActor actor, UUID targetUserId) {
        authorizeTarget(actor, targetUserId);
        return views(actor, targetUserId);
    }

    @Override
    @Transactional
    public void revoke(SessionActor actor, UUID sessionId) {
        AuthSessionEntity session = sessionRepository.findByIdForUpdate(sessionId)
                .orElseThrow(AuthSessionNotFoundException::new);
        RevocationDecision decision = authorizeSession(actor, session);
        if (!session.revoke(decision.reason(), decision.source(), actor.userId(), clock.instant())) {
            return;
        }
        sessionRepository.save(session);
        appendAudit(
                AuthSessionAuditEventType.SESSION_REVOKED,
                actor.userId(),
                session,
                decision.reason().name());
    }

    @Override
    @Transactional
    public void logoutAll(SessionActor actor) {
        List<AuthSessionEntity> sessions = sessionRepository.findByUserIdForUpdate(actor.userId());
        Instant now = clock.instant();
        boolean changed = false;
        for (AuthSessionEntity session : sessions) {
            changed |= session.revoke(
                    AuthSessionRevocationReason.LOGOUT_ALL,
                    AuthSessionRevocationSource.SELF,
                    actor.userId(),
                    now);
        }
        if (!changed) {
            return;
        }
        sessionRepository.saveAll(sessions);
        auditPort.append(new AuthSessionAuditEvent(
                actor.organizationId(),
                actor.userId(),
                actor.userId(),
                actor.currentSessionId(),
                null,
                AuthSessionAuditEventType.LOGOUT_ALL,
                "LOGOUT_ALL",
                now));
    }

    @Override
    @Transactional
    public void logout(JwtClaims claims) {
        UUID userId = parseUuid(claims.subject());
        UUID sessionId = parseUuid(claims.sessionId());
        if (sessionId == null) {
            revokeLegacyToken(claims, userId);
            return;
        }

        AuthSessionEntity session = sessionRepository.findByIdForUpdate(sessionId)
                .orElseThrow(AuthSessionNotFoundException::new);
        assertClaimsMatchSession(claims, userId, session);
        if (!session.revoke(
                AuthSessionRevocationReason.LOGOUT,
                AuthSessionRevocationSource.SELF,
                userId,
                clock.instant())) {
            return;
        }
        sessionRepository.save(session);
        appendAudit(AuthSessionAuditEventType.SESSION_REVOKED, userId, session, "LOGOUT");
    }

    private List<AuthSessionView> views(SessionActor actor, UUID targetUserId) {
        Instant now = clock.instant();
        return sessionRepository.findByUserIdOrderByCreatedAtDesc(targetUserId).stream()
                .map(session -> toView(session, actor.currentSessionId(), now))
                .toList();
    }

    private void authorizeTarget(SessionActor actor, UUID targetUserId) {
        if (actor.userId().equals(targetUserId)) {
            return;
        }
        if (!actor.canManageOtherUsers()) {
            throw new AuthSessionAccessDeniedException();
        }
        UserAccountEntity target = actor.organizationId() == null
                ? null
                : userRepository.findByIdAndOrganizationId(targetUserId, actor.organizationId()).orElse(null);
        if (target == null) {
            throw new AuthSessionNotFoundException();
        }
    }

    private RevocationDecision authorizeSession(SessionActor actor, AuthSessionEntity session) {
        UUID targetUserId = session.getUser().getId();
        if (actor.userId().equals(targetUserId)) {
            return new RevocationDecision(
                    AuthSessionRevocationReason.LOGOUT,
                    AuthSessionRevocationSource.SELF);
        }
        if (!Objects.equals(actor.organizationId(), session.getOrganizationId())) {
            throw new AuthSessionNotFoundException();
        }
        if (!actor.canManageOtherUsers()) {
            throw new AuthSessionAccessDeniedException();
        }
        return new RevocationDecision(
                AuthSessionRevocationReason.ADMIN_REVOKED,
                AuthSessionRevocationSource.ADMIN);
    }

    private void revokeLegacyToken(JwtClaims claims, UUID userId) {
        if (!jwtRevocationService.revoke(claims, userId, "LOGOUT")) {
            return;
        }
        if (userId == null) {
            return;
        }
        auditPort.append(new AuthSessionAuditEvent(
                parseUuid(claims.organizationId()),
                userId,
                userId,
                null,
                null,
                AuthSessionAuditEventType.LEGACY_ACCESS_TOKEN_REVOKED,
                "LOGOUT",
                clock.instant()));
    }

    private void assertClaimsMatchSession(
            JwtClaims claims,
            UUID userId,
            AuthSessionEntity session) {
        if (userId == null
                || !session.getUser().getId().equals(userId)
                || !Objects.equals(session.getOrganizationId(), parseUuid(claims.organizationId()))) {
            throw new AuthSessionNotFoundException();
        }
    }

    private void appendAudit(
            AuthSessionAuditEventType eventType,
            UUID actorUserId,
            AuthSessionEntity session,
            String reason) {
        auditPort.append(new AuthSessionAuditEvent(
                session.getOrganizationId(),
                actorUserId,
                session.getUser().getId(),
                session.getId(),
                session.getTokenFamilyId(),
                eventType,
                reason,
                clock.instant()));
    }

    private static AuthSessionView toView(
            AuthSessionEntity session,
            UUID currentSessionId,
            Instant now) {
        String state = session.getRevokedAt() != null
                ? "REVOKED"
                : session.isActiveAt(now) ? "ACTIVE" : "EXPIRED";
        Instant effectiveExpiry = session.getIdleExpiresAt().isBefore(session.getAbsoluteExpiresAt())
                ? session.getIdleExpiresAt()
                : session.getAbsoluteExpiresAt();
        return new AuthSessionView(
                session.getId(),
                session.getId().equals(currentSessionId),
                session.getClientType(),
                session.getUserAgent(),
                session.getNetworkPrefix(),
                session.getCreatedAt(),
                session.getLastUsedAt(),
                effectiveExpiry,
                state,
                session.getRevocationReason());
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

    private record RevocationDecision(
            AuthSessionRevocationReason reason,
            AuthSessionRevocationSource source) {
    }
}
