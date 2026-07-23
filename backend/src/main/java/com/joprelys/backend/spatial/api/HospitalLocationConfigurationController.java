package com.joprelys.backend.spatial.api;

import com.joprelys.backend.spatial.api.HospitalLocationDtos.FacilityLocationResponse;
import com.joprelys.backend.spatial.api.HospitalLocationDtos.FacilitySpaceResponse;
import com.joprelys.backend.spatial.api.HospitalLocationDtos.LocationTypeResponse;
import com.joprelys.backend.spatial.api.HospitalLocationDtos.SaveFacilityLocationRequest;
import com.joprelys.backend.spatial.api.HospitalLocationDtos.SaveFacilitySpaceRequest;
import com.joprelys.backend.spatial.api.HospitalLocationDtos.SaveUnitSpaceAssignmentRequest;
import com.joprelys.backend.spatial.api.HospitalLocationDtos.SpaceTypeResponse;
import com.joprelys.backend.spatial.api.HospitalLocationDtos.UnitSpaceAssignmentResponse;
import com.joprelys.backend.spatial.application.HospitalLocationConfigurationService;
import jakarta.validation.Valid;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/spatial/configuration")
@PreAuthorize("hasAuthority('SPATIAL_CONFIGURATION_MANAGE')")
public class HospitalLocationConfigurationController {

    private final HospitalLocationConfigurationService service;

    public HospitalLocationConfigurationController(HospitalLocationConfigurationService service) {
        this.service = service;
    }

    @GetMapping("/location-types")
    public List<LocationTypeResponse> listLocationTypes() {
        return service.listLocationTypes();
    }

    @GetMapping("/locations")
    public List<FacilityLocationResponse> listLocations(
            @RequestParam(required = false) UUID organizationId,
            @RequestParam(defaultValue = "false") boolean includeInactive) {
        return service.listLocations(organizationId, includeInactive);
    }

    @PostMapping("/locations")
    @ResponseStatus(HttpStatus.CREATED)
    public FacilityLocationResponse createLocation(
            @RequestParam(required = false) UUID organizationId,
            @Valid @RequestBody SaveFacilityLocationRequest request) {
        return service.createLocation(organizationId, request);
    }

    @PutMapping("/locations/{id}")
    public FacilityLocationResponse updateLocation(
            @RequestParam(required = false) UUID organizationId,
            @PathVariable UUID id,
            @Valid @RequestBody SaveFacilityLocationRequest request) {
        return service.updateLocation(organizationId, id, request);
    }

    @PostMapping("/locations/{id}/activate")
    public FacilityLocationResponse activateLocation(
            @RequestParam(required = false) UUID organizationId,
            @PathVariable UUID id) {
        return service.setLocationActive(organizationId, id, true);
    }

    @PostMapping("/locations/{id}/deactivate")
    public FacilityLocationResponse deactivateLocation(
            @RequestParam(required = false) UUID organizationId,
            @PathVariable UUID id) {
        return service.setLocationActive(organizationId, id, false);
    }

    @GetMapping("/space-types")
    public List<SpaceTypeResponse> listSpaceTypes() {
        return service.listSpaceTypes();
    }

    @GetMapping("/spaces")
    public List<FacilitySpaceResponse> listSpaces(
            @RequestParam(required = false) UUID organizationId,
            @RequestParam(required = false) UUID locationNodeId,
            @RequestParam(defaultValue = "false") boolean includeInactive) {
        return service.listSpaces(organizationId, locationNodeId, includeInactive);
    }

    @PostMapping("/spaces")
    @ResponseStatus(HttpStatus.CREATED)
    public FacilitySpaceResponse createSpace(
            @RequestParam(required = false) UUID organizationId,
            @Valid @RequestBody SaveFacilitySpaceRequest request) {
        return service.createSpace(organizationId, request);
    }

    @PutMapping("/spaces/{id}")
    public FacilitySpaceResponse updateSpace(
            @RequestParam(required = false) UUID organizationId,
            @PathVariable UUID id,
            @Valid @RequestBody SaveFacilitySpaceRequest request) {
        return service.updateSpace(organizationId, id, request);
    }

    @PostMapping("/spaces/{id}/activate")
    public FacilitySpaceResponse activateSpace(
            @RequestParam(required = false) UUID organizationId,
            @PathVariable UUID id) {
        return service.setSpaceActive(organizationId, id, true);
    }

    @PostMapping("/spaces/{id}/deactivate")
    public FacilitySpaceResponse deactivateSpace(
            @RequestParam(required = false) UUID organizationId,
            @PathVariable UUID id) {
        return service.setSpaceActive(organizationId, id, false);
    }

    @PostMapping("/spaces/{id}/inpatient-profile")
    public FacilitySpaceResponse enableInpatientProfile(
            @RequestParam(required = false) UUID organizationId,
            @PathVariable UUID id) {
        return service.enableInpatientProfile(organizationId, id);
    }

    @GetMapping("/unit-space-assignments")
    public List<UnitSpaceAssignmentResponse> listAssignments(
            @RequestParam(required = false) UUID organizationId,
            @RequestParam(required = false) UUID spaceId,
            @RequestParam(required = false) UUID unitId,
            @RequestParam(required = false) Instant activeAt) {
        return service.listAssignments(organizationId, spaceId, unitId, activeAt);
    }

    @PostMapping("/unit-space-assignments")
    @ResponseStatus(HttpStatus.CREATED)
    public UnitSpaceAssignmentResponse createAssignment(
            @RequestParam(required = false) UUID organizationId,
            @Valid @RequestBody SaveUnitSpaceAssignmentRequest request) {
        return service.createAssignment(organizationId, request);
    }

    @PutMapping("/unit-space-assignments/{id}")
    public UnitSpaceAssignmentResponse updateAssignment(
            @RequestParam(required = false) UUID organizationId,
            @PathVariable UUID id,
            @Valid @RequestBody SaveUnitSpaceAssignmentRequest request) {
        return service.updateAssignment(organizationId, id, request);
    }
}
