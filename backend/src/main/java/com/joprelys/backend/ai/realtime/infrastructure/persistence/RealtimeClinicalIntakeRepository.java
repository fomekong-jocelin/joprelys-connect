package com.joprelys.backend.ai.realtime.infrastructure.persistence;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface RealtimeClinicalIntakeRepository extends JpaRepository<RealtimeClinicalIntakeEntity, UUID> {

    Optional<RealtimeClinicalIntakeEntity> findByVisitIdAndEventId(UUID visitId, String eventId);

    Optional<RealtimeClinicalIntakeEntity> findByVisitIdAndItemId(UUID visitId, String itemId);

    List<RealtimeClinicalIntakeEntity> findByVisitIdOrderBySequenceNoAsc(UUID visitId);

    @Query("select coalesce(max(item.sequenceNo), 0) from RealtimeClinicalIntakeEntity item where item.visitId = :visitId")
    long findMaximumSequence(@Param("visitId") UUID visitId);
}
