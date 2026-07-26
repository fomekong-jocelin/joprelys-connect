package com.joprelys.backend.ai.ambient.infrastructure.persistence;

import com.joprelys.backend.ai.ambient.domain.AmbientTranscriptStatus;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface AmbientTranscriptItemRepository extends JpaRepository<AmbientTranscriptItemEntity, UUID> {

    Optional<AmbientTranscriptItemEntity> findByVisitIdAndSourceEventId(
            UUID visitId,
            String sourceEventId);

    List<AmbientTranscriptItemEntity> findByVisitIdAndStatusOrderByStartOffsetMsAscSequenceNoAsc(
            UUID visitId,
            AmbientTranscriptStatus status);

    @Query("select coalesce(max(item.sequenceNo), 0) from AmbientTranscriptItemEntity item where item.visitId = :visitId")
    long findMaximumSequence(@Param("visitId") UUID visitId);
}
