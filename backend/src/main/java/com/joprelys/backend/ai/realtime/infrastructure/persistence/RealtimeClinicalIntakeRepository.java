package com.joprelys.backend.ai.realtime.infrastructure.persistence;

import com.joprelys.backend.ai.realtime.application.RealtimeIntakeSource;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface RealtimeClinicalIntakeRepository extends JpaRepository<RealtimeClinicalIntakeEntity, UUID> {

    Optional<RealtimeClinicalIntakeEntity> findByVisitIdAndSourceAndEventId(
            UUID visitId,
            RealtimeIntakeSource source,
            String eventId);

    Optional<RealtimeClinicalIntakeEntity> findByVisitIdAndSourceAndItemId(
            UUID visitId,
            RealtimeIntakeSource source,
            String itemId);

    Optional<RealtimeClinicalIntakeEntity> findByIdAndVisitIdAndSource(
            UUID id,
            UUID visitId,
            RealtimeIntakeSource source);

    List<RealtimeClinicalIntakeEntity> findByVisitIdAndSourceOrderBySequenceNoAsc(
            UUID visitId,
            RealtimeIntakeSource source);

    List<RealtimeClinicalIntakeEntity> findByVisitIdAndSourceAndCaptureStatusNotOrderBySequenceNoAsc(
            UUID visitId,
            RealtimeIntakeSource source,
            String captureStatus);

    @Query("select coalesce(max(item.sequenceNo), 0) from RealtimeClinicalIntakeEntity item where item.visitId = :visitId")
    long findMaximumSequence(@Param("visitId") UUID visitId);
}
