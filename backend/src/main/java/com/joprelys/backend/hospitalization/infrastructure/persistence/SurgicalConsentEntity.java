package com.joprelys.backend.hospitalization.infrastructure.persistence;

import jakarta.persistence.*;
import org.hibernate.annotations.TenantId;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "surgical_consents")
public class SurgicalConsentEntity {

    @Id
    private UUID id;

    @Column(name = "hospitalization_id", nullable = false)
    private UUID hospitalizationId;

    @TenantId
    @Column(name = "organization_id")
    private UUID organizationId;

    @Version
    @Column(name = "version", nullable = false)
    private Long version;

    @Column(name = "consent_type", nullable = false, length = 50)
    private String consentType; // ANESTHESIA, SURGERY

    @Column(name = "patient_signature_present", nullable = false)
    private boolean patientSignaturePresent;

    @Column(name = "witness_name", length = 100)
    private String witnessName;

    @Column(name = "document_id")
    private UUID documentId;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected SurgicalConsentEntity() {
    }

    public SurgicalConsentEntity(UUID hospitalizationId, String consentType, boolean patientSignaturePresent, String witnessName) {
        this.id = UUID.randomUUID();
        this.hospitalizationId = hospitalizationId;
        this.consentType = consentType;
        this.patientSignaturePresent = patientSignaturePresent;
        this.witnessName = witnessName;
    }

    @PrePersist
    void prePersist() {
        Instant now = Instant.now();
        this.createdAt = now;
        this.updatedAt = now;
    }

    @PreUpdate
    void preUpdate() {
        this.updatedAt = Instant.now();
    }

    public UUID getId() {
        return id;
    }

    public UUID getHospitalizationId() {
        return hospitalizationId;
    }

    public UUID getOrganizationId() {
        return organizationId;
    }

    public Long getVersion() {
        return version;
    }

    public String getConsentType() {
        return consentType;
    }

    public boolean isPatientSignaturePresent() {
        return patientSignaturePresent;
    }

    public String getWitnessName() {
        return witnessName;
    }

    public UUID getDocumentId() {
        return documentId;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public void setDocumentId(UUID documentId) {
        this.documentId = documentId;
    }

    public void setWitnessName(String witnessName) {
        this.witnessName = witnessName;
    }

    public void setPatientSignaturePresent(boolean patientSignaturePresent) {
        this.patientSignaturePresent = patientSignaturePresent;
    }
}
