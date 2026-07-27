package com.joprelys.backend.ai.facts.infrastructure.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;
import org.hibernate.annotations.TenantId;

@Entity
@Table(name = "ai_clinical_note_validations")
public class ClinicalNoteValidationEntity {

    @Id
    private UUID id;

    @TenantId
    @Column(name = "organization_id", nullable = false)
    private UUID organizationId;

    @Column(name = "visit_id", nullable = false)
    private UUID visitId;

    @Column(name = "validation_request_id", nullable = false)
    private UUID validationRequestId;

    @Column(name = "projection_version", nullable = false, length = 128)
    private String projectionVersion;

    @Column(name = "projection_schema_version", nullable = false, length = 64)
    private String projectionSchemaVersion;

    @Column(name = "max_fact_sequence", nullable = false)
    private long maxFactSequence;

    @Column(name = "validated_by_user_id", nullable = false)
    private UUID validatedByUserId;

    @Column(name = "validated_at", nullable = false)
    private Instant validatedAt;

    protected ClinicalNoteValidationEntity() {
    }

    public ClinicalNoteValidationEntity(
            UUID organizationId,
            UUID visitId,
            UUID validationRequestId,
            String projectionVersion,
            String projectionSchemaVersion,
            long maxFactSequence,
            UUID validatedByUserId) {
        this.id = UUID.randomUUID();
        this.organizationId = organizationId;
        this.visitId = visitId;
        this.validationRequestId = validationRequestId;
        this.projectionVersion = projectionVersion;
        this.projectionSchemaVersion = projectionSchemaVersion;
        this.maxFactSequence = maxFactSequence;
        this.validatedByUserId = validatedByUserId;
    }

    @PrePersist
    void prePersist() {
        if (validatedAt == null) validatedAt = Instant.now();
    }

    public UUID getId() { return id; }
    public UUID getOrganizationId() { return organizationId; }
    public UUID getVisitId() { return visitId; }
    public UUID getValidationRequestId() { return validationRequestId; }
    public String getProjectionVersion() { return projectionVersion; }
    public String getProjectionSchemaVersion() { return projectionSchemaVersion; }
    public long getMaxFactSequence() { return maxFactSequence; }
    public UUID getValidatedByUserId() { return validatedByUserId; }
    public Instant getValidatedAt() { return validatedAt; }
}
