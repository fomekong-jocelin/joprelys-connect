package com.joprelys.backend.spatial.api;

import java.util.List;
import java.util.UUID;

public record RoomOccupancyResponse(
        UUID id,
        String roomNumber,
        Integer capacity,
        String comfortLevel,
        List<BedResponse> beds
) {
}
