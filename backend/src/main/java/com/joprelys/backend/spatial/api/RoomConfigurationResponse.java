package com.joprelys.backend.spatial.api;

import java.util.List;
import java.util.UUID;

public record RoomConfigurationResponse(
        UUID id,
        UUID wardId,
        String roomNumber,
        Integer capacity,
        String comfortLevel,
        List<BedResponse> beds
) {
}
