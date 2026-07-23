package com.joprelys.backend.spatial.api;

import java.util.List;
import java.util.UUID;

public record SpaceOccupancyResponse(
        UUID spaceId,
        String spaceCode,
        String spaceName,
        int installedBeds,
        int occupiedBeds,
        int availableBeds,
        int openBeds,
        int readyBeds,
        List<BedResponse> beds
) {
}
