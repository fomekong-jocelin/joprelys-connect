package com.joprelys.backend.spatial.api;

import com.joprelys.backend.spatial.domain.HospitalServiceType;
import java.util.List;
import java.util.UUID;

public record WardConfigurationResponse(
        UUID id,
        String name,
        HospitalServiceType serviceType,
        boolean allowsRooms,
        List<RoomConfigurationResponse> rooms
) {
}
