package com.joprelys.backend.patient.reconciliation.infrastructure.persistence;

import com.joprelys.backend.patient.infrastructure.persistence.PatientEntity;
import com.joprelys.backend.patient.reconciliation.domain.PatientAliasType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import java.time.Instant;
import java.util.UUID;
import org.hibernate.annotations.TenantId;

@Entity
@Table(name = "patient_identity_aliases")
public class PatientIdentityAliasEntity {

    @Id
    private UUID id;

    @TenantId
    @Column(name = "organization_id", nullable = false)
    private UUID organizationId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "origin_patient_id", nullable = false)
    private PatientEntity originPatient;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "canonical_patient_id", nullable = false)
    private PatientEntity canonicalPatient;

    @Enumerated(EnumType.STRING)
    @Column(name = "alias_type", nullable = false, length = 32)
    private PatientAliasType aliasType;

    @Column(name = "alias_value", nullable = false, length = 80)
    private String aliasValue;

    @Column(name = "created_by_user_id")
    private UUID createdByUserId;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @Version
    @Column(name = "version", nullable = false)
    private long version;

    protected PatientIdentityAliasEntity() {
    }

    public PatientIdentityAliasEntity(
            UUID organizationId,
            PatientEntity originPatient,
            PatientEntity canonicalPatient,
            PatientAliasType aliasType,
            String aliasValue,
            UUID createdByUserId) {
        this.id = UUID.randomUUID();
        this.organizationId = organizationId;
        this.originPatient = originPatient;
        this.canonicalPatient = canonicalPatient;
        this.aliasType = aliasType;
        this.aliasValue = requireText(aliasValue);
        this.createdByUserId = createdByUserId;
    }

    @PrePersist
    void prePersist() {
        Instant now = Instant.now();
        createdAt = now;
        updatedAt = now;
    }

    @PreUpdate
    void preUpdate() {
        updatedAt = Instant.now();
    }

    public void repointTo(PatientEntity canonicalPatient) {
        if (canonicalPatient == null) {
            throw new IllegalArgumentException("PATIENT_CANONICAL_TARGET_REQUIRED");
        }
        this.canonicalPatient = canonicalPatient;
    }

    private static String requireText(String value) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("PATIENT_ALIAS_VALUE_REQUIRED");
        }
        return value.trim();
    }

    public UUID getId() {
        return id;
    }

    public UUID getOrganizationId() {
        return organizationId;
    }

    public PatientEntity getOriginPatient() {
        return originPatient;
    }

    public PatientEntity getCanonicalPatient() {
        return canonicalPatient;
    }

    public PatientAliasType getAliasType() {
        return aliasType;
    }

    public String getAliasValue() {
        return aliasValue;
    }

    public UUID getCreatedByUserId() {
        return createdByUserId;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }
}
