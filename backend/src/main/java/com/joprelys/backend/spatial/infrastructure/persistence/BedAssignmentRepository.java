package com.joprelys.backend.spatial.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.Optional;
import java.util.UUID;

public interface BedAssignmentRepository extends JpaRepository<BedAssignmentEntity, UUID> {

    @Query("SELECT a FROM BedAssignmentEntity a WHERE a.hospitalizationId = :hospitalizationId AND a.releasedAt IS NULL")
    Optional<BedAssignmentEntity> findActiveByHospitalizationId(@Param("hospitalizationId") UUID hospitalizationId);

    @Query("SELECT a FROM BedAssignmentEntity a WHERE a.bed.id = :bedId AND a.releasedAt IS NULL")
    Optional<BedAssignmentEntity> findActiveByBedId(@Param("bedId") UUID bedId);
}
