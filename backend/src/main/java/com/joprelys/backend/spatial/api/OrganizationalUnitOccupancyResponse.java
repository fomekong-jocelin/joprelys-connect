package com.joprelys.backend.spatial.api;

import java.util.List;
import java.util.UUID;

public record OrganizationalUnitOccupancyResponse(
        UUID organizationalUnitId,
        String unitCode,
        String unitName,
        List<UUID> spaceIds,
        int installedBeds,
        int occupiedBeds,
        int availableBeds,
        int openBeds,
        int readyBeds
) {
}
