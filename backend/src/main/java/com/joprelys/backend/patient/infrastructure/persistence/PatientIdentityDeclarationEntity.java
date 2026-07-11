package com.joprelys.backend.patient.infrastructure.persistence;

import com.joprelys.backend.patient.domain.IdentityConfidenceLevel;
import com.joprelys.backend.patient.domain.IdentitySourceType;
import com.joprelys.backend.patient.domain.IdentityVerificationStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import java.time.Instant;
import java.util.UUID;
import org.hibernate.annotations.TenantId;

@Entity
@Table(name = "patient_identity_declarations")
public class PatientIdentityDeclarationEntity {

    @Id
    private UUID id;

    @TenantId
    @Column(name = "organization_id", nullable = false)
    private UUID organizationId;

    @Column(name = "patient_id", nullable = false)
    private UUID patientId;

    @Column(name = "field_name", nullable = false, length = 80)
    private String fieldName;

    @Column(name = "declared_value", nullable = false, columnDefinition = "TEXT")
    private String declaredValue;

    @Enumerated(EnumType.STRING)
    @Column(name = "source_type", nullable = false, length = 40)
    private IdentitySourceType sourceType;

    @Column(name = "source_details", length = 500)
    private String sourceDetails;

    @Enumerated(EnumType.STRING)
    @Column(name = "confidence_level", nullable = false, length = 24)
    private IdentityConfidenceLevel confidenceLevel;

    @Enumerated(EnumType.STRING)
    @Column(name = "verification_status", nullable = false, length = 24)
    private IdentityVerificationStatus verificationStatus;

    @Column(name = "declared_by", nullable = false)
    private UUID declaredBy;

    @Column(name = "declared_at", nullable = false)
    private Instant declaredAt;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Version
    @Column(nullable = false)
    private long version;

    protected PatientIdentityDeclarationEntity() {
    }

    public PatientIdentityDeclarationEntity(
            UUID organizationId,
            UUID patientId,
            String fieldName,
            String declaredValue,
            IdentitySourceType sourceType,
            String sourceDetails,
            IdentityConfidenceLevel confidenceLevel,
            IdentityVerificationStatus verificationStatus,
            UUID declaredBy,
            Instant declaredAt) {
        this.id = UUID.randomUUID();
        this.organizationId = organizationId;
        this.patientId = patientId;
        this.fieldName = fieldName;
        this.declaredValue = declaredValue;
        this.sourceType = sourceType;
        this.sourceDetails = sourceDetails;
        this.confidenceLevel = confidenceLevel;
        this.verificationStatus = verificationStatus;
        this.declaredBy = declaredBy;
        this.declaredAt = declaredAt;
    }

    @PrePersist
    void prePersist() {
        if (declaredAt == null) {
            declaredAt = Instant.now();
        }
        createdAt = Instant.now();
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

    public String getFieldName() {
        return fieldName;
    }

    public String getDeclaredValue() {
        return declaredValue;
    }

    public IdentitySourceType getSourceType() {
        return sourceType;
    }

    public String getSourceDetails() {
        return sourceDetails;
    }

    public IdentityConfidenceLevel getConfidenceLevel() {
        return confidenceLevel;
    }

    public IdentityVerificationStatus getVerificationStatus() {
        return verificationStatus;
    }

    public UUID getDeclaredBy() {
        return declaredBy;
    }

    public Instant getDeclaredAt() {
        return declaredAt;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public long getVersion() {
        return version;
    }
}
