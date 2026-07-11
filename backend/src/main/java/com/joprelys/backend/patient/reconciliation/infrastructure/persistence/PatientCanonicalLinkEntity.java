package com.joprelys.backend.patient.reconciliation.infrastructure.persistence;

import com.joprelys.backend.patient.domain.PatientIdentityStatus;
import com.joprelys.backend.patient.infrastructure.persistence.PatientEntity;
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
import jakarta.persistence.Version;
import java.time.Instant;
import java.util.UUID;
import org.hibernate.annotations.TenantId;

@Entity
@Table(name = "patient_canonical_links")
public class PatientCanonicalLinkEntity {

    @Id
    private UUID id;

    @TenantId
    @Column(name = "organization_id", nullable = false)
    private UUID organizationId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "source_patient_id", nullable = false)
    private PatientEntity sourcePatient;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "canonical_patient_id", nullable = false)
    private PatientEntity canonicalPatient;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "decision_event_id", nullable = false)
    private PatientReconciliationEventEntity decisionEvent;

    @Enumerated(EnumType.STRING)
    @Column(name = "source_previous_identity_status", nullable = false, length = 32)
    private PatientIdentityStatus sourcePreviousIdentityStatus;

    @Column(name = "linked_at", nullable = false)
    private Instant linkedAt;

    @Version
    @Column(name = "version", nullable = false)
    private long version;

    protected PatientCanonicalLinkEntity() {
    }

    public PatientCanonicalLinkEntity(
            UUID organizationId,
            PatientEntity sourcePatient,
            PatientEntity canonicalPatient,
            PatientReconciliationEventEntity decisionEvent,
            PatientIdentityStatus sourcePreviousIdentityStatus) {
        this.id = UUID.randomUUID();
        this.organizationId = organizationId;
        this.sourcePatient = sourcePatient;
        this.canonicalPatient = canonicalPatient;
        this.decisionEvent = decisionEvent;
        this.sourcePreviousIdentityStatus = sourcePreviousIdentityStatus;
    }

    @PrePersist
    void prePersist() {
        linkedAt = Instant.now();
    }

    public UUID getId() {
        return id;
    }

    public UUID getOrganizationId() {
        return organizationId;
    }

    public PatientEntity getSourcePatient() {
        return sourcePatient;
    }

    public PatientEntity getCanonicalPatient() {
        return canonicalPatient;
    }

    public PatientReconciliationEventEntity getDecisionEvent() {
        return decisionEvent;
    }

    public PatientIdentityStatus getSourcePreviousIdentityStatus() {
        return sourcePreviousIdentityStatus;
    }

    public Instant getLinkedAt() {
        return linkedAt;
    }
}
