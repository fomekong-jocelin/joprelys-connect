package com.joprelys.backend.spatial.application;

import com.joprelys.backend.spatial.api.HospitalLocationDtos.FacilitySpaceResponse;
import com.joprelys.backend.spatial.api.SpaceOccupancyResponse;
import com.joprelys.backend.spatial.infrastructure.persistence.BedAssignmentRepository;
import com.joprelys.backend.spatial.infrastructure.persistence.BedEntity;
import com.joprelys.backend.spatial.infrastructure.persistence.BedRepository;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Charge en trois requêtes (chambres, lits, affectations actives) ce que l'écran de suivi
 * obtenait auparavant avec une requête par chambre.
 */
@Service
class DefaultInpatientOccupancyQuery implements InpatientOccupancyQuery {

    private final HospitalLocationConfigurationService locationConfigurationService;
    private final BedRepository bedRepository;
    private final BedAssignmentRepository bedAssignmentRepository;

    DefaultInpatientOccupancyQuery(
            HospitalLocationConfigurationService locationConfigurationService,
            BedRepository bedRepository,
            BedAssignmentRepository bedAssignmentRepository) {
        this.locationConfigurationService = locationConfigurationService;
        this.bedRepository = bedRepository;
        this.bedAssignmentRepository = bedAssignmentRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public List<SpaceOccupancyResponse> listInpatientOccupancy() {
        List<FacilitySpaceResponse> spaces = locationConfigurationService.listSpaces(null, null, false).stream()
                .filter(space -> space.inpatientProfile() && space.active())
                .toList();
        if (spaces.isEmpty()) {
            return List.of();
        }

        List<BedEntity> beds = bedRepository.findBySpaceIdInOrderByBedNumberAsc(
                spaces.stream().map(FacilitySpaceResponse::id).toList());
        Map<UUID, List<BedEntity>> bedsBySpace = beds.stream()
                .collect(Collectors.groupingBy(bed -> bed.getSpace().getId()));
        Set<UUID> activeBedIds = beds.isEmpty() ? Set.of() : new HashSet<>(
                bedAssignmentRepository.findActiveBedIds(beds.stream().map(BedEntity::getId).toList()));

        return spaces.stream()
                .map(space -> toResponse(space, bedsBySpace.getOrDefault(space.id(), List.of()), activeBedIds))
                .toList();
    }

    private static SpaceOccupancyResponse toResponse(
            FacilitySpaceResponse space,
            List<BedEntity> beds,
            Set<UUID> activeBedIds) {
        SpatialBedMetrics metrics = SpatialBedMetrics.calculate(beds, activeBedIds);
        return new SpaceOccupancyResponse(
                space.id(),
                space.code(),
                space.name(),
                metrics.installedBeds(),
                metrics.occupiedBeds(),
                metrics.availableBeds(),
                metrics.openBeds(),
                metrics.readyBeds(),
                metrics.bedResponses());
    }
}
