package com.joprelys.backend.billing.infrastructure.persistence;

import jakarta.persistence.*;
import org.hibernate.annotations.TenantId;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "receivable_reminders")
public class ReceivableReminderEntity {

    @Id
    private UUID id;

    @Column(name = "receivable_id", nullable = false)
    private UUID receivableId;

    @Enumerated(EnumType.STRING)
    @Column(name = "action_type", nullable = false, length = 50)
    private ReceivableReminderActionType actionType;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 50)
    private ReceivableReminderStatus status;

    @Column(name = "notes")
    private String notes;

    @Column(name = "actor_id", nullable = false)
    private UUID actorId;

    @TenantId
    @Column(name = "organization_id")
    private UUID organizationId;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Version
    private Long version;

    protected ReceivableReminderEntity() {
    }

    public ReceivableReminderEntity(UUID receivableId, ReceivableReminderActionType actionType, ReceivableReminderStatus status, String notes, UUID actorId) {
        this.id = UUID.randomUUID();
        this.receivableId = receivableId;
        this.actionType = actionType;
        this.status = status;
        this.notes = notes;
        this.actorId = actorId;
    }

    @PrePersist
    void prePersist() {
        this.createdAt = Instant.now();
    }

    public UUID getId() {
        return id;
    }

    public UUID getReceivableId() {
        return receivableId;
    }

    public ReceivableReminderActionType getActionType() {
        return actionType;
    }

    public void setActionType(ReceivableReminderActionType actionType) {
        this.actionType = actionType;
    }

    public ReceivableReminderStatus getStatus() {
        return status;
    }

    public void setStatus(ReceivableReminderStatus status) {
        this.status = status;
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }

    public UUID getActorId() {
        return actorId;
    }

    public void setActorId(UUID actorId) {
        this.actorId = actorId;
    }

    public UUID getOrganizationId() {
        return organizationId;
    }

    public void setOrganizationId(UUID organizationId) {
        this.organizationId = organizationId;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Long getVersion() {
        return version;
    }
}
