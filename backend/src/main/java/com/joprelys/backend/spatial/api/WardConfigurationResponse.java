package com.joprelys.backend.spatial.api;

import java.util.List;
import java.util.UUID;

public record WardConfigurationResponse(
        UUID id,
        String name,
        List<RoomConfigurationResponse> rooms
) {
}
