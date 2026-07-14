package com.joprelys.backend.auth.session.infrastructure.persistence;

import com.joprelys.backend.auth.session.domain.AuthSessionAuditEvent;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "auth_session_audit_events")
public class AuthSessionAuditEventEntity {

    @Id
    private UUID id;

    @Column(name = "organization_id")
    private UUID organizationId;

    @Column(name = "actor_user_id")
    private UUID actorUserId;

    @Column(name = "target_user_id", nullable = false)
    private UUID targetUserId;

    @Column(name = "session_id")
    private UUID sessionId;

    @Column(name = "token_family_id")
    private UUID tokenFamilyId;

    @Column(name = "event_type", nullable = false, length = 48)
    private String eventType;

    @Column(length = 64)
    private String reason;

    @Column(name = "occurred_at", nullable = false)
    private Instant occurredAt;

    protected AuthSessionAuditEventEntity() {
    }

    private AuthSessionAuditEventEntity(AuthSessionAuditEvent event) {
        this.id = UUID.randomUUID();
        this.organizationId = event.organizationId();
        this.actorUserId = event.actorUserId();
        this.targetUserId = event.targetUserId();
        this.sessionId = event.sessionId();
        this.tokenFamilyId = event.tokenFamilyId();
        this.eventType = event.eventType().name();
        this.reason = event.reason();
        this.occurredAt = event.occurredAt();
    }

    public static AuthSessionAuditEventEntity from(AuthSessionAuditEvent event) {
        return new AuthSessionAuditEventEntity(event);
    }

    public String getEventType() {
        return eventType;
    }

    public UUID getTokenFamilyId() {
        return tokenFamilyId;
    }
}
