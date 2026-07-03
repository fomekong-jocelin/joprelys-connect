package com.joprelys.backend.hospitalization.api;

import com.joprelys.backend.hospitalization.infrastructure.persistence.HospitalizationNoteEntity;
import java.time.Instant;
import java.util.UUID;

public record HospitalizationNoteResponse(
        UUID id,
        UUID hospitalizationId,
        UUID organizationId,
        String authorName,
        String noteContent,
        Instant createdAt
) {
    public static HospitalizationNoteResponse fromEntity(HospitalizationNoteEntity entity) {
        return new HospitalizationNoteResponse(
                entity.getId(),
                entity.getHospitalizationId(),
                entity.getOrganizationId(),
                entity.getAuthorName(),
                entity.getNoteContent(),
                entity.getCreatedAt()
        );
    }
}
