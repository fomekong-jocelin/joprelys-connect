package com.joprelys.backend.ai.facts.infrastructure.persistence;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ClinicalNoteValidationRepository
        extends JpaRepository<ClinicalNoteValidationEntity, UUID> {

    Optional<ClinicalNoteValidationEntity> findByVisitIdAndValidationRequestId(
            UUID visitId,
            UUID validationRequestId);

    Optional<ClinicalNoteValidationEntity> findByVisitIdAndProjectionVersionAndValidatedByUserId(
            UUID visitId,
            String projectionVersion,
            UUID validatedByUserId);

    List<ClinicalNoteValidationEntity> findByVisitIdOrderByValidatedAtDesc(UUID visitId);
}
