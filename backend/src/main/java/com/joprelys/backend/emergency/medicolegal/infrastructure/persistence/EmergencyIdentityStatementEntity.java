package com.joprelys.backend.emergency.medicolegal.infrastructure.persistence;

import com.joprelys.backend.emergency.infrastructure.persistence.EmergencyEntity;
import com.joprelys.backend.patient.domain.IdentityConfidenceLevel;
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
@Table(name = "emergency_identity_statements")
public class EmergencyIdentityStatementEntity {

    @Id
    private UUID id;

    @TenantId
    @Column(name = "organization_id", nullable = false)
    private UUID organizationId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "emergency_id", nullable = false)
    private EmergencyEntity emergency;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "third_party_id")
    private EmergencyThirdPartyEntity thirdParty;

    @Column(name = "field_name", nullable = false, length = 80)
    private String fieldName;

    @Column(name = "declared_value", nullable = false, columnDefinition = "TEXT")
    private String declaredValue;

    @Enumerated(EnumType.STRING)
    @Column(name = "confidence_level", nullable = false, length = 24)
    private IdentityConfidenceLevel confidenceLevel;

    @Column(name = "proof_reference", length = 255)
    private String proofReference;

    @Column(name = "declared_at", nullable = false)
    private Instant declaredAt;

    @Column(name = "created_by_user_id")
    private UUID createdByUserId;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    protected EmergencyIdentityStatementEntity() {
    }

    public EmergencyIdentityStatementEntity(
            UUID organizationId,
            EmergencyEntity emergency,
            EmergencyThirdPartyEntity thirdParty,
            String fieldName,
            String declaredValue,
            IdentityConfidenceLevel confidenceLevel,
            String proofReference,
            Instant declaredAt,
            UUID createdByUserId) {
        this.id = UUID.randomUUID();
        this.organizationId = organizationId;
        this.emergency = emergency;
        this.thirdParty = thirdParty;
        this.fieldName = requireText(fieldName, "EMERGENCY_IDENTITY_FIELD_REQUIRED");
        this.declaredValue = requireText(declaredValue, "EMERGENCY_IDENTITY_VALUE_REQUIRED");
        this.confidenceLevel = confidenceLevel;
        this.proofReference = normalize(proofReference);
        this.declaredAt = declaredAt == null ? Instant.now() : declaredAt;
        this.createdByUserId = createdByUserId;
    }

    @PrePersist
    void prePersist() {
        createdAt = Instant.now();
    }

    private static String requireText(String value, String errorCode) {
        String normalized = normalize(value);
        if (normalized == null) throw new IllegalArgumentException(errorCode);
        return normalized;
    }

    private static String normalize(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    public UUID getId() { return id; }
    public UUID getOrganizationId() { return organizationId; }
    public EmergencyEntity getEmergency() { return emergency; }
    public EmergencyThirdPartyEntity getThirdParty() { return thirdParty; }
    public String getFieldName() { return fieldName; }
    public String getDeclaredValue() { return declaredValue; }
    public IdentityConfidenceLevel getConfidenceLevel() { return confidenceLevel; }
    public String getProofReference() { return proofReference; }
    public Instant getDeclaredAt() { return declaredAt; }
    public UUID getCreatedByUserId() { return createdByUserId; }
    public Instant getCreatedAt() { return createdAt; }
}
