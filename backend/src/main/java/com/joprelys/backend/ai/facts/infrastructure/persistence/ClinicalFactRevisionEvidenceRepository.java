package com.joprelys.backend.ai.facts.infrastructure.persistence;

import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ClinicalFactRevisionEvidenceRepository
        extends JpaRepository<ClinicalFactRevisionEvidenceEntity, UUID> {

    List<ClinicalFactRevisionEvidenceEntity> findByOperationIdOrderByPrimarySupportDescQuoteStartCharAsc(
            UUID operationId);
}
