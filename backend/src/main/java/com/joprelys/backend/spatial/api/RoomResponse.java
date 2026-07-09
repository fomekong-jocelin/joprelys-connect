package com.joprelys.backend.spatial.api;

import com.joprelys.backend.spatial.infrastructure.persistence.RoomEntity;
import java.util.UUID;

public record RoomResponse(
        UUID id,
        UUID wardId,
        String roomNumber,
        Integer capacity,
        String comfortLevel
) {
    public static RoomResponse fromEntity(RoomEntity entity) {
        return new RoomResponse(
                entity.getId(),
                entity.getWard().getId(),
                entity.getRoomNumber(),
                entity.getCapacity(),
                entity.getComfortLevel()
        );
    }
}
