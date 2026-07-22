package com.joprelys.backend.spatial.infrastructure.persistence;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface BedAssignmentRepository extends JpaRepository<BedAssignmentEntity, UUID> {

    @Query("SELECT a FROM BedAssignmentEntity a WHERE a.hospitalizationId = :hospitalizationId AND a.releasedAt IS NULL")
    Optional<BedAssignmentEntity> findActiveByHospitalizationId(@Param("hospitalizationId") UUID hospitalizationId);

    @Query("SELECT a FROM BedAssignmentEntity a WHERE a.bed.id = :bedId AND a.releasedAt IS NULL")
    Optional<BedAssignmentEntity> findActiveByBedId(@Param("bedId") UUID bedId);

    @Query("SELECT a.bed.id FROM BedAssignmentEntity a WHERE a.bed.id IN :bedIds AND a.releasedAt IS NULL")
    List<UUID> findActiveBedIds(@Param("bedIds") Collection<UUID> bedIds);

    boolean existsByBedId(UUID bedId);
}
