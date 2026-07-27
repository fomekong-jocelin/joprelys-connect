package com.joprelys.backend.ai.facts.infrastructure.persistence;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ClinicalFactRevisionBatchRepository
        extends JpaRepository<ClinicalFactRevisionBatchEntity, UUID> {

    Optional<ClinicalFactRevisionBatchEntity> findByVisitIdAndRevisionRequestId(
            UUID visitId,
            UUID revisionRequestId);

    List<ClinicalFactRevisionBatchEntity> findByVisitIdOrderByCreatedAtDesc(UUID visitId);
}
