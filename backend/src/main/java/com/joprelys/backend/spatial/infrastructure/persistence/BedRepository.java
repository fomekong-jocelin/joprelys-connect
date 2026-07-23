package com.joprelys.backend.spatial.infrastructure.persistence;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

public interface BedRepository extends JpaRepository<BedEntity, UUID> {

    List<BedEntity> findBySpaceId(UUID spaceId);

    long countBySpaceId(UUID spaceId);

    boolean existsBySpaceId(UUID spaceId);

    boolean existsBySpaceIdAndBedNumberIgnoreCase(UUID spaceId, String bedNumber);

    boolean existsBySpaceIdAndBedNumberIgnoreCaseAndIdNot(UUID spaceId, String bedNumber, UUID id);

    Optional<BedEntity> findByIdAndOrganizationId(UUID id, UUID organizationId);

    @Transactional
    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("""
            UPDATE BedEntity b
            SET b.status = :occupiedStatus
            WHERE b.id = :bedId
              AND b.status = :freeStatus
              AND b.capacityStatus = :openCapacityStatus
              AND b.readinessStatus = :readyStatus
            """)
    int claimIfAvailable(
            @Param("bedId") UUID bedId,
            @Param("freeStatus") BedStatus freeStatus,
            @Param("occupiedStatus") BedStatus occupiedStatus,
            @Param("openCapacityStatus") BedCapacityStatus openCapacityStatus,
            @Param("readyStatus") BedReadinessStatus readyStatus);
}
