package com.joprelys.backend.spatial.application;
import com.joprelys.backend.spatial.api.BedResponse;
import com.joprelys.backend.spatial.infrastructure.persistence.BedEntity;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;
record SpatialBedMetrics(int installedBeds, int occupiedBeds, int availableBeds,
        int openBeds, int readyBeds, List<BedResponse> bedResponses) {
    static SpatialBedMetrics calculate(List<BedEntity> beds, Set<UUID> activeBedIds) {
        int open = 0;
        int ready = 0;
        int occupied = 0;
        int available = 0;
        List<BedResponse> responses = new ArrayList<>();
        for (BedEntity bed : beds) {
            boolean hasActiveAssignment = activeBedIds.contains(bed.getId());
            if (bed.isOpen()) {
                open++;
            }
            if (bed.isOpen() && bed.isReady()) {
                ready++;
            }
            if (hasActiveAssignment) {
                occupied++;
            }
            if (bed.isOperationallyAvailable(hasActiveAssignment)) {
                available++;
            }
            responses.add(BedResponse.fromEntity(bed, hasActiveAssignment));
        }
        return new SpatialBedMetrics(beds.size(), occupied, available, open, ready, responses);
    }
}
