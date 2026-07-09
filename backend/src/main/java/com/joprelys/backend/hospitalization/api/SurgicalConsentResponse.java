package com.joprelys.backend.hospitalization.api;

import com.joprelys.backend.hospitalization.infrastructure.persistence.SurgicalConsentEntity;
import java.time.Instant;
import java.util.UUID;

public record SurgicalConsentResponse(
        UUID id,
        UUID hospitalizationId,
        UUID organizationId,
        Long version,
        String consentType,
        boolean patientSignaturePresent,
        String witnessName,
        UUID documentId,
        Instant createdAt,
        Instant updatedAt
) {
    public static SurgicalConsentResponse fromEntity(SurgicalConsentEntity entity) {
        return new SurgicalConsentResponse(
                entity.getId(),
                entity.getHospitalizationId(),
                entity.getOrganizationId(),
                entity.getVersion(),
                entity.getConsentType(),
                entity.isPatientSignaturePresent(),
                entity.getWitnessName(),
                entity.getDocumentId(),
                entity.getCreatedAt(),
                entity.getUpdatedAt()
        );
    }
}
