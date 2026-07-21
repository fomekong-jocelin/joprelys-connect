package com.joprelys.backend.spatial.infrastructure.persistence;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface BedRepository extends JpaRepository<BedEntity, UUID> {
    List<BedEntity> findByRoomId(UUID roomId);

    long countByRoomId(UUID roomId);

    boolean existsByRoomId(UUID roomId);

    boolean existsByRoomIdAndBedNumberIgnoreCase(UUID roomId, String bedNumber);

    boolean existsByRoomIdAndBedNumberIgnoreCaseAndIdNot(UUID roomId, String bedNumber, UUID id);

    @Query("SELECT b FROM BedEntity b JOIN b.room r WHERE r.ward.id = :wardId")
    List<BedEntity> findByWardId(@Param("wardId") UUID wardId);

    @Query("""
            SELECT b
            FROM BedEntity b
            JOIN b.room r
            JOIN r.ward w
            WHERE b.organizationId = :organizationId
              AND r.organizationId = :organizationId
              AND w.organizationId = :organizationId
              AND LOWER(w.name) = LOWER(:wardName)
              AND LOWER(r.roomNumber) = LOWER(:roomNumber)
              AND LOWER(b.bedNumber) = LOWER(:bedNumber)
            """)
    Optional<BedEntity> findConfiguredBed(
            @Param("organizationId") UUID organizationId,
            @Param("wardName") String wardName,
            @Param("roomNumber") String roomNumber,
            @Param("bedNumber") String bedNumber);

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
