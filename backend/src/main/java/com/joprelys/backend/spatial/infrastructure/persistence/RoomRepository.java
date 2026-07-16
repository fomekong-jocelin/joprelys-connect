package com.joprelys.backend.spatial.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface RoomRepository extends JpaRepository<RoomEntity, UUID> {
    List<RoomEntity> findByWardId(UUID wardId);
    Optional<RoomEntity> findByRoomNumberIgnoreCase(String roomNumber);
    boolean existsByWardId(UUID wardId);
    boolean existsByWardIdAndRoomNumberIgnoreCase(UUID wardId, String roomNumber);
    boolean existsByWardIdAndRoomNumberIgnoreCaseAndIdNot(UUID wardId, String roomNumber, UUID id);
}
