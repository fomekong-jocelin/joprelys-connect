package com.joprelys.backend.spatial.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.UUID;

public interface RoomRepository extends JpaRepository<RoomEntity, UUID> {
    List<RoomEntity> findByWardId(UUID wardId);
}
