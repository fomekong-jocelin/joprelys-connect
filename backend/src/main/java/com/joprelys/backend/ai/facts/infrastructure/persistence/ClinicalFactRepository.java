package com.joprelys.backend.ai.facts.infrastructure.persistence;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ClinicalFactRepository extends JpaRepository<ClinicalFactEntity, UUID> {

    Optional<ClinicalFactEntity> findByVisitIdAndSourceEventId(UUID visitId, String sourceEventId);

    Optional<ClinicalFactEntity> findByIdAndVisitId(UUID id, UUID visitId);

    Optional<ClinicalFactEntity> findByVisitIdAndSupersedesFactId(UUID visitId, UUID supersedesFactId);

    List<ClinicalFactEntity> findByVisitIdOrderBySequenceNoAsc(UUID visitId);

    @Query("select coalesce(max(fact.sequenceNo), 0) from ClinicalFactEntity fact where fact.visitId = :visitId")
    long findMaximumSequence(@Param("visitId") UUID visitId);
}
