package com.joprelys.backend.patient.api;

import com.joprelys.backend.patient.infrastructure.persistence.ExternalAccessRequestEntity;
import java.time.Instant;
import java.util.UUID;

public record ExternalAccessResponse(
        UUID id,
        UUID patientId,
        UUID requesterUserId,
        UUID requesterOrganizationId,
        String requesterOrganizationName,
        String reason,
        Integer durationHours,
        String status,
        Instant createdAt,
        Instant expiresAt,
        String scopes
) {
    public static ExternalAccessResponse fromEntity(ExternalAccessRequestEntity entity, String requesterOrganizationName) {
        return new ExternalAccessResponse(
                entity.getId(),
                entity.getPatientId(),
                entity.getRequesterUserId(),
                entity.getRequesterOrganizationId(),
                requesterOrganizationName,
                entity.getReason(),
                entity.getRequestedDurationHours(),
                entity.getStatus(),
                entity.getCreatedAt(),
                entity.getExpiresAt(),
                entity.getScopes()
        );
    }
}
