package com.joprelys.backend.ai.facts.infrastructure.persistence;

import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ClinicalFactExtractionRepository extends JpaRepository<ClinicalFactExtractionEntity, UUID> {

    Optional<ClinicalFactExtractionEntity> findByVisitIdAndTranscriptItemIdAndExtractorVersion(
            UUID visitId,
            UUID transcriptItemId,
            String extractorVersion);
}
