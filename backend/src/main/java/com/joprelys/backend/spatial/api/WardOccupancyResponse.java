package com.joprelys.backend.spatial.api;

import java.util.List;
import java.util.UUID;

public record WardOccupancyResponse(
        UUID id,
        String name,
        List<RoomOccupancyResponse> rooms,
        Integer totalBedsCount,
        Integer occupiedBedsCount,
        Integer availableBedsCount,
        Integer openBedsCount,
        Integer readyBedsCount
) {
}
