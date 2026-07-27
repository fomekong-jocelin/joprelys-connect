package com.joprelys.backend.ai.facts.infrastructure.persistence;

import com.joprelys.backend.ai.facts.application.ClinicalFactRevisionContract.OperationType;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ClinicalFactRevisionOperationRepository
        extends JpaRepository<ClinicalFactRevisionOperationEntity, UUID> {

    List<ClinicalFactRevisionOperationEntity> findByBatchIdOrderByPositionNoAsc(UUID batchId);

    Optional<ClinicalFactRevisionOperationEntity> findByVisitIdAndOperationRequestId(
            UUID visitId,
            UUID operationRequestId);

    List<ClinicalFactRevisionOperationEntity> findByVisitIdAndOperationTypeOrderByCreatedAtAsc(
            UUID visitId,
            OperationType operationType);
}
