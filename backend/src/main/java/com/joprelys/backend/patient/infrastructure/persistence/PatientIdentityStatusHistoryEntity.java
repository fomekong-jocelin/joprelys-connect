package com.joprelys.backend.patient.infrastructure.persistence;

import com.joprelys.backend.patient.domain.PatientIdentityStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;
import org.hibernate.annotations.TenantId;

@Entity
@Table(name = "patient_identity_status_history")
public class PatientIdentityStatusHistoryEntity {

    @Id
    private UUID id;

    @TenantId
    @Column(name = "organization_id", nullable = false)
    private UUID organizationId;

    @Column(name = "patient_id", nullable = false)
    private UUID patientId;

    @Enumerated(EnumType.STRING)
    @Column(name = "previous_status", length = 32)
    private PatientIdentityStatus previousStatus;

    @Enumerated(EnumType.STRING)
    @Column(name = "new_status", nullable = false, length = 32)
    private PatientIdentityStatus newStatus;

    @Column(name = "reason", nullable = false, length = 500)
    private String reason;

    @Column(name = "changed_by", nullable = false)
    private UUID changedBy;

    @Column(name = "changed_at", nullable = false)
    private Instant changedAt;

    protected PatientIdentityStatusHistoryEntity() {
    }

    public PatientIdentityStatusHistoryEntity(
            UUID organizationId,
            UUID patientId,
            PatientIdentityStatus previousStatus,
            PatientIdentityStatus newStatus,
            String reason,
            UUID changedBy) {
        this.id = UUID.randomUUID();
        this.organizationId = organizationId;
        this.patientId = patientId;
        this.previousStatus = previousStatus;
        this.newStatus = newStatus;
        this.reason = reason;
        this.changedBy = changedBy;
    }

    @PrePersist
    void prePersist() {
        changedAt = Instant.now();
    }

    public UUID getId() {
        return id;
    }

    public UUID getOrganizationId() {
        return organizationId;
    }

    public UUID getPatientId() {
        return patientId;
    }

    public PatientIdentityStatus getPreviousStatus() {
        return previousStatus;
    }

    public PatientIdentityStatus getNewStatus() {
        return newStatus;
    }

    public String getReason() {
        return reason;
    }

    public UUID getChangedBy() {
        return changedBy;
    }

    public Instant getChangedAt() {
        return changedAt;
    }
}
