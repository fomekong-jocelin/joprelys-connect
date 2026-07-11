package com.joprelys.backend.spatial.infrastructure.persistence;

import jakarta.persistence.LockModeType;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
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

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT b FROM BedEntity b WHERE b.id = :bedId")
    Optional<BedEntity> findByIdForUpdate(@Param("bedId") UUID bedId);
}
