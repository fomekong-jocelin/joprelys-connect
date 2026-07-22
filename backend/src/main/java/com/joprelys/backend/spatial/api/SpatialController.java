package com.joprelys.backend.spatial.api;

import com.joprelys.backend.spatial.application.SpatialService;
import com.joprelys.backend.spatial.infrastructure.persistence.BedCapacityStatus;
import com.joprelys.backend.spatial.infrastructure.persistence.BedReadinessStatus;
import com.joprelys.backend.spatial.infrastructure.persistence.BedStateReasonCode;
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

    private final SpatialService spatialService;

    public SpatialController(SpatialService spatialService) {
        this.spatialService = spatialService;
    }

    @GetMapping("/wards")
    @PreAuthorize("hasAuthority('HOSPITALIZATION_READ')")
    public List<WardResponse> listWards() {
        return spatialService.listWards();
    }

    @GetMapping("/wards/{id}/occupancy")
    @PreAuthorize("hasAuthority('HOSPITALIZATION_READ')")
    public WardOccupancyResponse getWardOccupancy(@PathVariable UUID id) {
        return spatialService.getWardOccupancy(id);
    }

    @GetMapping("/beds/{id}/state-history")
    @PreAuthorize("hasAuthority('HOSPITALIZATION_READ')")
    public List<BedStateChangeResponse> getBedStateHistory(@PathVariable UUID id) {
        return spatialService.getBedStateHistory(id);
    }

    @PostMapping("/beds/{id}/status")
    @PreAuthorize("hasAuthority('BED_OPERATIONAL_STATUS_MANAGE')")
    public BedResponse updateBedStatus(
            @PathVariable UUID id,
            @Valid @RequestBody UpdateBedStatusRequest request) {
        try {
            BedStatus status = BedStatus.valueOf(request.status().toUpperCase());
            return spatialService.updateBedStatus(id, status, request.note());
        } catch (IllegalArgumentException exception) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Statut de lit invalide: " + request.status());
        }
    }

    @PostMapping("/beds/{id}/cleaning-status")
    @PreAuthorize("hasAuthority('BED_CLEANING_MANAGE')")
    public BedResponse updateBedCleaningStatus(
            @PathVariable UUID id,
            @Valid @RequestBody UpdateBedCleaningStatusRequest request) {
        BedReadinessStatus status = readinessStatus(request.status(), "nettoyage");
        if (status != BedReadinessStatus.CLEANING && status != BedReadinessStatus.READY) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Le circuit de nettoyage accepte uniquement CLEANING ou READY.");
        }
        return spatialService.updateBedCleaningStatus(
                id,
                status,
                reasonCode(request.reasonCode()),
                request.note());
    }

    @PostMapping("/beds/{id}/maintenance-status")
    @PreAuthorize("hasAuthority('BED_MAINTENANCE_MANAGE')")
    public BedResponse updateBedMaintenanceStatus(
            @PathVariable UUID id,
            @Valid @RequestBody UpdateBedMaintenanceStatusRequest request) {
        BedReadinessStatus status = readinessStatus(request.status(), "maintenance");
        if (status != BedReadinessStatus.MAINTENANCE && status != BedReadinessStatus.READY) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Le circuit de maintenance accepte uniquement MAINTENANCE ou READY.");
        }
        return spatialService.updateBedMaintenanceStatus(
                id,
                status,
                reasonCode(request.reasonCode()),
                request.note());
    }

    @PostMapping("/beds/{id}/capacity-status")
    @PreAuthorize("hasAuthority('BED_OPERATIONAL_STATUS_MANAGE')")
    public BedResponse updateBedCapacityStatus(
            @PathVariable UUID id,
            @Valid @RequestBody UpdateBedCapacityStatusRequest request) {
        try {
            BedCapacityStatus status = BedCapacityStatus.valueOf(request.status().toUpperCase());
            return spatialService.updateBedCapacityStatus(
                    id,
                    status,
                    reasonCode(request.reasonCode()),
                    request.note());
        } catch (IllegalArgumentException exception) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "État de capacité du lit invalide: " + request.status());
        }
    }

    @PostMapping("/transfers")
    @PreAuthorize("hasAuthority('HOSPITALIZATION_TRANSFER')")
    public BedAssignmentResponse transferPatient(@Valid @RequestBody TransferRequest request) {
        return spatialService.transferPatient(request.hospitalizationId(), request.newBedId());
    }

    private BedReadinessStatus readinessStatus(String value, String operation) {
        try {
            return BedReadinessStatus.valueOf(value.toUpperCase());
        } catch (IllegalArgumentException | NullPointerException exception) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "État de " + operation + " du lit invalide: " + value);
        }
    }

    private BedStateReasonCode reasonCode(String value) {
        return spatialService.parseBedStateReasonCode(value);
    }
}
