package com.joprelys.backend.auth.session.infrastructure.persistence;

import com.joprelys.backend.auth.infrastructure.persistence.UserAccountEntity;
import com.joprelys.backend.auth.session.application.SessionClientMetadata;
import com.joprelys.backend.auth.session.domain.AuthSessionRevocationReason;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

@Entity
@Table(name = "auth_sessions")
public class AuthSessionEntity {

    @Id
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private UserAccountEntity user;

    @Column(name = "organization_id")
    private UUID organizationId;

    @Column(name = "token_family_id", nullable = false)
    private UUID tokenFamilyId;

    @Column(name = "refresh_token_hash", nullable = false, unique = true, length = 64)
    private String refreshTokenHash;

    @Column(name = "client_type", nullable = false, length = 32)
    private String clientType;

    @Column(name = "user_agent", length = 160)
    private String userAgent;

    @Column(name = "ip_prefix", length = 64)
    private String networkPrefix;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "last_used_at", nullable = false)
    private Instant lastUsedAt;

    @Column(name = "absolute_expires_at", nullable = false)
    private Instant absoluteExpiresAt;

    @Column(name = "idle_expires_at", nullable = false)
    private Instant idleExpiresAt;

    @Column(name = "revoked_at")
    private Instant revokedAt;

    @Column(name = "revocation_reason", length = 32)
    private String revocationReason;

    @Column(name = "replaced_by_session_id")
    private UUID replacedBySessionId;

    @Version
    @Column(nullable = false)
    private long version;

    protected AuthSessionEntity() {
    }

    private AuthSessionEntity(
            UUID id,
            UserAccountEntity user,
            UUID organizationId,
            UUID tokenFamilyId,
            String refreshTokenHash,
            SessionClientMetadata metadata,
            Instant createdAt,
            Instant absoluteExpiresAt,
            Instant idleExpiresAt) {
        this.id = Objects.requireNonNull(id);
        this.user = Objects.requireNonNull(user);
        this.organizationId = organizationId;
        this.tokenFamilyId = Objects.requireNonNull(tokenFamilyId);
        this.refreshTokenHash = Objects.requireNonNull(refreshTokenHash);
        this.clientType = metadata.clientType();
        this.userAgent = metadata.userAgent();
        this.networkPrefix = metadata.networkPrefix();
        this.createdAt = Objects.requireNonNull(createdAt);
        this.lastUsedAt = createdAt;
        this.absoluteExpiresAt = Objects.requireNonNull(absoluteExpiresAt);
        this.idleExpiresAt = Objects.requireNonNull(idleExpiresAt);
    }

    public static AuthSessionEntity create(
            UserAccountEntity user,
            UUID tokenFamilyId,
            String refreshTokenHash,
            SessionClientMetadata metadata,
            Instant createdAt,
            Instant absoluteExpiresAt,
            Instant idleExpiresAt) {
        return new AuthSessionEntity(
                UUID.randomUUID(),
                user,
                user.getOrganizationId(),
                tokenFamilyId,
                refreshTokenHash,
                metadata,
                createdAt,
                absoluteExpiresAt,
                idleExpiresAt);
    }

    public boolean isActiveAt(Instant now) {
        return revokedAt == null
                && now.isBefore(absoluteExpiresAt)
                && now.isBefore(idleExpiresAt);
    }

    public void replaceWith(AuthSessionEntity replacement, Instant now) {
        if (!isActiveAt(now)) {
            throw new IllegalStateException("AUTH_SESSION_NOT_ACTIVE");
        }
        if (!tokenFamilyId.equals(replacement.tokenFamilyId)) {
            throw new IllegalArgumentException("AUTH_SESSION_FAMILY_MISMATCH");
        }
        lastUsedAt = now;
        revokedAt = now;
        revocationReason = AuthSessionRevocationReason.ROTATED.name();
        replacedBySessionId = replacement.id;
    }

    public void revoke(AuthSessionRevocationReason reason, Instant now) {
        if (revokedAt != null) {
            return;
        }
        revokedAt = Objects.requireNonNull(now);
        revocationReason = Objects.requireNonNull(reason).name();
    }

    public UUID getId() {
        return id;
    }

    public UserAccountEntity getUser() {
        return user;
    }

    public UUID getOrganizationId() {
        return organizationId;
    }

    public UUID getTokenFamilyId() {
        return tokenFamilyId;
    }

    public String getRefreshTokenHash() {
        return refreshTokenHash;
    }

    public String getClientType() {
        return clientType;
    }

    public String getUserAgent() {
        return userAgent;
    }

    public String getNetworkPrefix() {
        return networkPrefix;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getLastUsedAt() {
        return lastUsedAt;
    }

    public Instant getAbsoluteExpiresAt() {
        return absoluteExpiresAt;
    }

    public Instant getIdleExpiresAt() {
        return idleExpiresAt;
    }

    public Instant getRevokedAt() {
        return revokedAt;
    }

    public String getRevocationReason() {
        return revocationReason;
    }

    public UUID getReplacedBySessionId() {
        return replacedBySessionId;
    }

    public long getVersion() {
        return version;
    }
}
