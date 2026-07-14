package com.joprelys.backend.auth.session.infrastructure.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

@Entity
@Table(name = "revoked_access_tokens")
public class RevokedAccessTokenEntity {

    @Id
    @Column(name = "token_id", length = 64)
    private String tokenId;

    @Column(name = "user_id")
    private UUID userId;

    @Column(name = "organization_id")
    private UUID organizationId;

    @Column(name = "revoked_at", nullable = false)
    private Instant revokedAt;

    @Column(name = "expires_at", nullable = false)
    private Instant expiresAt;

    @Column(nullable = false, length = 32)
    private String reason;

    @Column(name = "revoked_by_user_id")
    private UUID revokedByUserId;

    protected RevokedAccessTokenEntity() {
    }

    public RevokedAccessTokenEntity(
            String tokenId,
            UUID userId,
            UUID organizationId,
            Instant revokedAt,
            Instant expiresAt,
            String reason,
            UUID revokedByUserId) {
        this.tokenId = Objects.requireNonNull(tokenId);
        this.userId = userId;
        this.organizationId = organizationId;
        this.revokedAt = Objects.requireNonNull(revokedAt);
        this.expiresAt = Objects.requireNonNull(expiresAt);
        this.reason = Objects.requireNonNull(reason);
        this.revokedByUserId = revokedByUserId;
    }
}
