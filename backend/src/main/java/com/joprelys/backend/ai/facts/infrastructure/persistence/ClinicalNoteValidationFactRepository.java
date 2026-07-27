package com.joprelys.backend.ai.facts.infrastructure.persistence;

import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ClinicalNoteValidationFactRepository
        extends JpaRepository<ClinicalNoteValidationFactEntity, UUID> {

    List<ClinicalNoteValidationFactEntity> findByValidationIdOrderBySectionCodeAscPositionNoAsc(UUID validationId);
}
