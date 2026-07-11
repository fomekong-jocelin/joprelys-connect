package com.joprelys.backend.spatial.api;

import com.joprelys.backend.spatial.application.SpatialService;
import com.joprelys.backend.spatial.infrastructure.persistence.BedStatus;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/spatial")
public class SpatialController {

    private static final String LEGACY_READ_ROLES =
            "hasAnyRole('AGENT_ACCUEIL', 'INFIRMIER', 'MEDECIN', 'ADMIN_CLINIQUE', 'ADMIN_JOPRELYS', 'SUPER_ADMIN')";
    private static final String LEGACY_MANAGE_ROLES =
            "hasAnyRole('INFIRMIER', 'MEDECIN', 'ADMIN_CLINIQUE', 'ADMIN_JOPRELYS', 'SUPER_ADMIN')";

    private final SpatialService spatialService;

    public SpatialController(SpatialService spatialService) {
        this.spatialService = spatialService;
    }

    @GetMapping("/wards")
    @PreAuthorize("hasAuthority('HOSPITALIZATION_READ') or " + LEGACY_READ_ROLES)
    public List<WardResponse> listWards() {
        return spatialService.listWards();
    }

    @GetMapping("/wards/{id}/occupancy")
    @PreAuthorize("hasAuthority('HOSPITALIZATION_READ') or " + LEGACY_READ_ROLES)
    public WardOccupancyResponse getWardOccupancy(@PathVariable UUID id) {
        return spatialService.getWardOccupancy(id);
    }

    @PostMapping("/beds/{id}/status")
    @PreAuthorize("hasAuthority('HOSPITALIZATION_MANAGE') or " + LEGACY_MANAGE_ROLES)
    public BedResponse updateBedStatus(
            @PathVariable UUID id,
            @Valid @RequestBody UpdateBedStatusRequest request) {
        try {
            BedStatus status = BedStatus.valueOf(request.status().toUpperCase());
            return spatialService.updateBedStatus(id, status);
        } catch (IllegalArgumentException exception) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Statut de lit invalide: " + request.status());
        }
    }

    @PostMapping("/transfers")
    @PreAuthorize("hasAuthority('HOSPITALIZATION_MANAGE') or " + LEGACY_MANAGE_ROLES)
    public BedAssignmentResponse transferPatient(@Valid @RequestBody TransferRequest request) {
        return spatialService.transferPatient(request.hospitalizationId(), request.newBedId());
    }
}
