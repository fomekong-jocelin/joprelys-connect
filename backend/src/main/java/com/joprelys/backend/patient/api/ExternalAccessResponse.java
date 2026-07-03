package com.joprelys.backend.patient.api;

import com.joprelys.backend.patient.infrastructure.persistence.ExternalAccessRequestEntity;
import java.time.Instant;
import java.util.UUID;

public record ExternalAccessResponse(
        UUID id,
        UUID patientId,
        UUID requesterUserId,
        UUID requesterOrganizationId,
        String reason,
        Integer durationHours,
        String status,
        Instant createdAt,
        Instant expiresAt
) {
    public static ExternalAccessResponse fromEntity(ExternalAccessRequestEntity entity) {
        return new ExternalAccessResponse(
                entity.getId(),
                entity.getPatientId(),
                entity.getRequesterUserId(),
                entity.getRequesterOrganizationId(),
                entity.getReason(),
                entity.getRequestedDurationHours(),
                entity.getStatus(),
                entity.getCreatedAt(),
                entity.getExpiresAt()
        );
    }
}
