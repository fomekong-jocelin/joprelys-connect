package com.joprelys.backend.auth.session.application;

import com.joprelys.backend.auth.infrastructure.persistence.UserAccountEntity;
import com.joprelys.backend.auth.security.JwtService;
import com.joprelys.backend.auth.session.domain.AuthSessionAuditEvent;
import com.joprelys.backend.auth.session.domain.AuthSessionAuditEventType;
import com.joprelys.backend.auth.session.domain.AuthSessionRevocationReason;
import com.joprelys.backend.auth.session.domain.AuthSessionRevocationSource;
import com.joprelys.backend.auth.session.infrastructure.persistence.AuthSessionEntity;
import com.joprelys.backend.auth.session.infrastructure.persistence.AuthSessionRepository;
import com.joprelys.backend.clinic.infrastructure.persistence.OrganizationRepository;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PersistentAuthSessionService
        implements IssueAuthSessionUseCase, RefreshAuthSessionUseCase {

    private static final Duration CONCURRENT_REFRESH_WINDOW = Duration.ofSeconds(10);

    private final AuthSessionRepository sessionRepository;
    private final RefreshTokenGenerator tokenGenerator;
    private final RefreshTokenHasher tokenHasher;
    private final AuthSessionExpiryPolicy expiryPolicy;
    private final AuthSessionAuditPort auditPort;
    private final JwtService jwtService;
    private final OrganizationRepository organizationRepository;
    private final Clock clock;

    public PersistentAuthSessionService(
            AuthSessionRepository sessionRepository,
            RefreshTokenGenerator tokenGenerator,
            RefreshTokenHasher tokenHasher,
            AuthSessionExpiryPolicy expiryPolicy,
            AuthSessionAuditPort auditPort,
            JwtService jwtService,
            OrganizationRepository organizationRepository,
            Clock clock) {
        this.sessionRepository = sessionRepository;
        this.tokenGenerator = tokenGenerator;
        this.tokenHasher = tokenHasher;
        this.expiryPolicy = expiryPolicy;
        this.auditPort = auditPort;
        this.jwtService = jwtService;
        this.organizationRepository = organizationRepository;
        this.clock = clock;
    }

    @Override
    @Transactional
    public IssuedAuthSession issue(UserAccountEntity user, SessionClientMetadata metadata) {
        assertAccountCanAuthenticate(user);
        Instant now = clock.instant();
        Instant absoluteExpiry = expiryPolicy.absoluteExpiryFrom(now);
        Instant idleExpiry = expiryPolicy.idleExpiryFrom(now, absoluteExpiry);
        String refreshToken = tokenGenerator.generate();
        AuthSessionEntity session = AuthSessionEntity.create(
                user,
                UUID.randomUUID(),
                tokenHasher.hash(refreshToken),
                metadata,
                now,
                absoluteExpiry,
                idleExpiry);
        sessionRepository.save(session);
        appendAudit(AuthSessionAuditEventType.SESSION_CREATED, session, user.getId(), null, now);
        return issuedSession(session, refreshToken);
    }

    @Override
    @Transactional(noRollbackFor = {InvalidAuthSessionException.class, ConcurrentAuthRefreshException.class})
    public IssuedAuthSession refresh(String refreshToken, SessionClientMetadata metadata) {
        String tokenHash = hashRequiredToken(refreshToken);
        AuthSessionEntity current = sessionRepository.findByRefreshTokenHashForUpdate(tokenHash)
                .orElseThrow(InvalidAuthSessionException::new);
        Instant now = clock.instant();
        if (isRotated(current)) {
            if (isLikelyConcurrentRefresh(current, metadata, now)) {
                throw new ConcurrentAuthRefreshException();
            }
            handleReplay(current, now);
            throw new InvalidAuthSessionException();
        }
        assertSessionCanRotate(current, now);

        String replacementToken = tokenGenerator.generate();
        Instant idleExpiry = expiryPolicy.idleExpiryFrom(now, current.getAbsoluteExpiresAt());
        AuthSessionEntity replacement = AuthSessionEntity.create(
                current.getUser(),
                current.getTokenFamilyId(),
                tokenHasher.hash(replacementToken),
                metadata,
                now,
                current.getAbsoluteExpiresAt(),
                idleExpiry);

        sessionRepository.saveAndFlush(replacement);
        current.replaceWith(replacement, now);
        sessionRepository.save(current);
        appendAudit(
                AuthSessionAuditEventType.SESSION_ROTATED,
                current,
                current.getUser().getId(),
                AuthSessionRevocationReason.ROTATED.name(),
                now);
        return issuedSession(replacement, replacementToken);
    }

    private boolean isLikelyConcurrentRefresh(
            AuthSessionEntity consumedSession,
            SessionClientMetadata metadata,
            Instant now) {
        Instant rotatedAt = consumedSession.getRevokedAt();
        if (rotatedAt == null
                || now.isBefore(rotatedAt)
                || Duration.between(rotatedAt, now).compareTo(CONCURRENT_REFRESH_WINDOW) > 0) {
            return false;
        }
        return Objects.equals(consumedSession.getClientType(), metadata.clientType())
                && Objects.equals(consumedSession.getUserAgent(), metadata.userAgent())
                && Objects.equals(consumedSession.getNetworkPrefix(), metadata.networkPrefix());
    }

    private void handleReplay(AuthSessionEntity consumedSession, Instant now) {
        List<AuthSessionEntity> family = sessionRepository.findByTokenFamilyIdForUpdate(
                consumedSession.getTokenFamilyId());
        boolean changed = false;
        for (AuthSessionEntity session : family) {
            changed |= session.revoke(
                    AuthSessionRevocationReason.REPLAY_DETECTED,
                    AuthSessionRevocationSource.SYSTEM,
                    null,
                    now);
        }
        if (!changed) {
            return;
        }
        sessionRepository.saveAll(family);
        if (auditPort.exists(
                AuthSessionAuditEventType.REFRESH_REPLAY_DETECTED,
                consumedSession.getTokenFamilyId())) {
            return;
        }
        appendAudit(
                AuthSessionAuditEventType.REFRESH_REPLAY_DETECTED,
                consumedSession,
                null,
                AuthSessionRevocationReason.REPLAY_DETECTED.name(),
                now);
    }

    private IssuedAuthSession issuedSession(AuthSessionEntity session, String refreshToken) {
        UserAccountEntity user = session.getUser();
        JwtService.CreatedToken accessToken = jwtService.createToken(user, session.getId());
        return new IssuedAuthSession(
                accessToken.value(),
                accessToken.expiresAt(),
                session.getId(),
                session.getIdleExpiresAt(),
                refreshToken,
                user.getEmail(),
                user.getDisplayName(),
                user.getRole());
    }

    private String hashRequiredToken(String refreshToken) {
        if (refreshToken == null || refreshToken.isBlank()) {
            throw new InvalidAuthSessionException();
        }
        return tokenHasher.hash(refreshToken);
    }

    private void assertSessionCanRotate(AuthSessionEntity session, Instant now) {
        if (!session.isActiveAt(now)) {
            throw new InvalidAuthSessionException();
        }
        UserAccountEntity user = session.getUser();
        assertAccountCanAuthenticate(user);
        if (!Objects.equals(session.getOrganizationId(), user.getOrganizationId())) {
            throw new InvalidAuthSessionException();
        }
    }

    private void assertAccountCanAuthenticate(UserAccountEntity user) {
        if (user == null || !user.isEnabled()) {
            throw new InvalidAuthSessionException();
        }
        UUID organizationId = user.getOrganizationId();
        if (organizationId == null) {
            return;
        }
        boolean activeOrganization = organizationRepository.findById(organizationId)
                .map(organization -> "ACTIVE".equals(organization.getStatus()))
                .orElse(false);
        if (!activeOrganization) {
            throw new InvalidAuthSessionException();
        }
    }

    private void appendAudit(
            AuthSessionAuditEventType eventType,
            AuthSessionEntity session,
            UUID actorUserId,
            String reason,
            Instant occurredAt) {
        auditPort.append(new AuthSessionAuditEvent(
                session.getOrganizationId(),
                actorUserId,
                session.getUser().getId(),
                session.getId(),
                session.getTokenFamilyId(),
                eventType,
                reason,
                occurredAt));
    }

    private static boolean isRotated(AuthSessionEntity session) {
        return AuthSessionRevocationReason.ROTATED.name().equals(session.getRevocationReason());
    }
}
