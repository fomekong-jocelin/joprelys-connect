package com.joprelys.backend.ai.facts.infrastructure.persistence;

import com.joprelys.backend.ai.facts.application.ClinicalFactRevisionContract.OperationType;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ClinicalFactRevisionOperationRepository
        extends JpaRepository<ClinicalFactRevisionOperationEntity, UUID> {

    List<ClinicalFactRevisionOperationEntity> findByBatchIdOrderByPositionNoAsc(UUID batchId);

    Optional<ClinicalFactRevisionOperationEntity> findByVisitIdAndOperationRequestId(
            UUID visitId,
            UUID operationRequestId);

    @Query("""
            select operation.targetFactId
            from ClinicalFactRevisionOperationEntity operation
            where operation.visitId = :visitId
              and operation.operationType = :operationType
              and operation.targetFactId is not null
            """)
    List<UUID> findTargetFactIdsByVisitIdAndOperationType(
            @Param("visitId") UUID visitId,
            @Param("operationType") OperationType operationType);
}
