package com.joprelys.backend.emergency.medicolegal.infrastructure.persistence;

import com.joprelys.backend.emergency.medicolegal.domain.EmergencyBelongingTransferAction;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;
import org.hibernate.annotations.TenantId;

@Entity
@Table(name = "emergency_belonging_transfers")
public class EmergencyBelongingTransferEntity {

    @Id
    private UUID id;

    @TenantId
    @Column(name = "organization_id", nullable = false)
    private UUID organizationId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "belonging_id", nullable = false)
    private EmergencyBelongingEntity belonging;

    @Enumerated(EnumType.STRING)
    @Column(name = "action_type", nullable = false, length = 32)
    private EmergencyBelongingTransferAction actionType;

    @Column(name = "from_custodian", length = 160)
    private String fromCustodian;

    @Column(name = "recipient_name", length = 160)
    private String recipientName;

    @Column(name = "recipient_id_document", length = 120)
    private String recipientIdDocument;

    @Column(name = "notes", length = 1000)
    private String notes;

    @Column(name = "occurred_at", nullable = false)
    private Instant occurredAt;

    @Column(name = "performed_by_user_id")
    private UUID performedByUserId;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    protected EmergencyBelongingTransferEntity() {
    }

    public EmergencyBelongingTransferEntity(
            UUID organizationId,
            EmergencyBelongingEntity belonging,
            EmergencyBelongingTransferAction actionType,
            String fromCustodian,
            String recipientName,
            String recipientIdDocument,
            String notes,
            Instant occurredAt,
            UUID performedByUserId) {
        this.id = UUID.randomUUID();
        this.organizationId = organizationId;
        this.belonging = belonging;
        this.actionType = actionType;
        this.fromCustodian = normalize(fromCustodian);
        this.recipientName = normalize(recipientName);
        this.recipientIdDocument = normalize(recipientIdDocument);
        this.notes = normalize(notes);
        this.occurredAt = occurredAt == null ? Instant.now() : occurredAt;
        this.performedByUserId = performedByUserId;
    }

    @PrePersist
    void prePersist() {
        createdAt = Instant.now();
    }

    private static String normalize(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    public UUID getId() { return id; }
    public UUID getOrganizationId() { return organizationId; }
    public EmergencyBelongingEntity getBelonging() { return belonging; }
    public EmergencyBelongingTransferAction getActionType() { return actionType; }
    public String getFromCustodian() { return fromCustodian; }
    public String getRecipientName() { return recipientName; }
    public String getRecipientIdDocument() { return recipientIdDocument; }
    public String getNotes() { return notes; }
    public Instant getOccurredAt() { return occurredAt; }
    public UUID getPerformedByUserId() { return performedByUserId; }
    public Instant getCreatedAt() { return createdAt; }
}
