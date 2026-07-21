package com.joprelys.backend.emergency.document;

import com.joprelys.backend.visit.infrastructure.persistence.DocumentStatus;
import com.joprelys.backend.visit.infrastructure.persistence.DocumentType;
import com.joprelys.backend.visit.infrastructure.persistence.MedicalDocumentEntity;
import java.time.Instant;
import java.util.UUID;

public record EmergencyDocumentResponse(
        UUID id,
        UUID visitId,
        UUID originPatientId,
        String documentNumber,
        DocumentType documentType,
        DocumentStatus status,
        String hash,
        int version,
        String verificationUrl,
        Instant createdAt) {

    public static EmergencyDocumentResponse fromEntity(MedicalDocumentEntity entity) {
        return new EmergencyDocumentResponse(
                entity.getId(),
                entity.getVisit().getId(),
                entity.getVisit().getPatient().getId(),
                entity.getDocumentNumber(),
                entity.getDocumentType(),
                entity.getStatus(),
                entity.getHash(),
                entity.getVersion(),
                entity.getVerificationUrl(),
                entity.getCreatedAt());
    }
}
