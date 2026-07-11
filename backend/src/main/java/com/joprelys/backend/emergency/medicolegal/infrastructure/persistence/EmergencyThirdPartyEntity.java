package com.joprelys.backend.emergency.medicolegal.infrastructure.persistence;

import com.joprelys.backend.emergency.infrastructure.persistence.EmergencyEntity;
import com.joprelys.backend.emergency.medicolegal.domain.EmergencyInformationSourceType;
import com.joprelys.backend.emergency.medicolegal.domain.EmergencyThirdPartyQuality;
import com.joprelys.backend.patient.domain.IdentityConfidenceLevel;
import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
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
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import org.hibernate.annotations.TenantId;

@Entity
@Table(name = "emergency_third_parties")
public class EmergencyThirdPartyEntity {

    @Id
    private UUID id;

    @TenantId
    @Column(name = "organization_id", nullable = false)
    private UUID organizationId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "emergency_id", nullable = false)
    private EmergencyEntity emergency;

    @Column(name = "full_name", nullable = false, length = 160)
    private String fullName;

    @Column(name = "phone", length = 40)
    private String phone;

    @Column(name = "email", length = 255)
    private String email;

    @Column(name = "id_document", length = 120)
    private String idDocument;

    @Column(name = "relationship_to_patient", length = 80)
    private String relationshipToPatient;

    @Column(name = "circumstances", length = 1000)
    private String circumstances;

    @Column(name = "consent_to_contact", nullable = false)
    private boolean consentToContact;

    @Column(name = "legal_representative_claimed", nullable = false)
    private boolean legalRepresentativeClaimed;

    @Enumerated(EnumType.STRING)
    @Column(name = "source_type", nullable = false, length = 40)
    private EmergencyInformationSourceType sourceType;

    @Enumerated(EnumType.STRING)
    @Column(name = "confidence_level", nullable = false, length = 24)
    private IdentityConfidenceLevel confidenceLevel;

    @Column(name = "proof_reference", length = 255)
    private String proofReference;

    @ElementCollection(fetch = FetchType.LAZY)
    @CollectionTable(
            name = "emergency_third_party_qualities",
            joinColumns = @JoinColumn(name = "third_party_id"))
    @Enumerated(EnumType.STRING)
    @Column(name = "quality", nullable = false, length = 48)
    private Set<EmergencyThirdPartyQuality> qualities = new HashSet<>();

    @Column(name = "created_by_user_id")
    private UUID createdByUserId;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @Version
    @Column(name = "version", nullable = false)
    private long version;

    protected EmergencyThirdPartyEntity() {
    }

    public EmergencyThirdPartyEntity(
            UUID organizationId,
            EmergencyEntity emergency,
            String fullName,
            String phone,
            String email,
            String idDocument,
            String relationshipToPatient,
            String circumstances,
            boolean consentToContact,
            boolean legalRepresentativeClaimed,
            EmergencyInformationSourceType sourceType,
            IdentityConfidenceLevel confidenceLevel,
            String proofReference,
            Set<EmergencyThirdPartyQuality> qualities,
            UUID createdByUserId) {
        this.id = UUID.randomUUID();
        this.organizationId = organizationId;
        this.emergency = emergency;
        this.fullName = requireText(fullName);
        this.phone = normalize(phone);
        this.email = normalize(email);
        this.idDocument = normalize(idDocument);
        this.relationshipToPatient = normalize(relationshipToPatient);
        this.circumstances = normalize(circumstances);
        this.consentToContact = consentToContact;
        this.legalRepresentativeClaimed = legalRepresentativeClaimed;
        this.sourceType = sourceType;
        this.confidenceLevel = confidenceLevel;
        this.proofReference = normalize(proofReference);
        this.qualities.addAll(qualities == null ? Set.of() : qualities);
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

    private static String requireText(String value) {
        String normalized = normalize(value);
        if (normalized == null) {
            throw new IllegalArgumentException("EMERGENCY_THIRD_PARTY_NAME_REQUIRED");
        }
        return normalized;
    }

    private static String normalize(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    public UUID getId() { return id; }
    public UUID getOrganizationId() { return organizationId; }
    public EmergencyEntity getEmergency() { return emergency; }
    public String getFullName() { return fullName; }
    public String getPhone() { return phone; }
    public String getEmail() { return email; }
    public String getIdDocument() { return idDocument; }
    public String getRelationshipToPatient() { return relationshipToPatient; }
    public String getCircumstances() { return circumstances; }
    public boolean isConsentToContact() { return consentToContact; }
    public boolean isLegalRepresentativeClaimed() { return legalRepresentativeClaimed; }
    public EmergencyInformationSourceType getSourceType() { return sourceType; }
    public IdentityConfidenceLevel getConfidenceLevel() { return confidenceLevel; }
    public String getProofReference() { return proofReference; }
    public Set<EmergencyThirdPartyQuality> getQualities() { return Set.copyOf(qualities); }
    public UUID getCreatedByUserId() { return createdByUserId; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
}
