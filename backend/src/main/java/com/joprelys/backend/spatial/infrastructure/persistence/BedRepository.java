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

    @Query("SELECT b FROM BedEntity b JOIN b.room r WHERE r.ward.id = :wardId")
    List<BedEntity> findByWardId(@Param("wardId") UUID wardId);

    @Query("SELECT b FROM BedEntity b JOIN b.room r JOIN r.ward w WHERE w.name = :wardName AND r.roomNumber = :roomNumber AND b.bedNumber = :bedNumber")
    Optional<BedEntity> findByWardRoomAndBedNumber(
            @Param("wardName") String wardName,
            @Param("roomNumber") String roomNumber,
            @Param("bedNumber") String bedNumber);

    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("UPDATE BedEntity b SET b.status = :occupiedStatus WHERE b.id = :bedId AND b.status = :freeStatus")
    int claimIfFree(
            @Param("bedId") UUID bedId,
            @Param("freeStatus") BedStatus freeStatus,
            @Param("occupiedStatus") BedStatus occupiedStatus);
}
