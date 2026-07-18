package com.joprelys.backend.spatial.api;

import com.joprelys.backend.spatial.application.SpatialConfigurationUseCase;
import jakarta.validation.Valid;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
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
public class SpatialConfigurationController {

    private final SpatialConfigurationUseCase configurationUseCase;

    public SpatialConfigurationController(SpatialConfigurationUseCase configurationUseCase) {
        this.configurationUseCase = configurationUseCase;
    }

    @GetMapping
    public SpatialConfigurationResponse getConfiguration(@RequestParam(required = false) UUID organizationId) {
        return configurationUseCase.getConfiguration(organizationId);
    }

    @PostMapping("/wards")
    @ResponseStatus(HttpStatus.CREATED)
    public WardResponse createWard(
            @RequestParam(required = false) UUID organizationId,
            @Valid @RequestBody SaveWardRequest request) {
        return configurationUseCase.createWard(organizationId, request);
    }

    @PutMapping("/wards/{id}")
    public WardResponse updateWard(
            @RequestParam(required = false) UUID organizationId,
            @PathVariable UUID id,
            @Valid @RequestBody SaveWardRequest request) {
        return configurationUseCase.updateWard(organizationId, id, request);
    }

    @DeleteMapping("/wards/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteWard(@RequestParam(required = false) UUID organizationId, @PathVariable UUID id) {
        configurationUseCase.deleteWard(organizationId, id);
    }

    @PostMapping("/rooms")
    @ResponseStatus(HttpStatus.CREATED)
    public RoomResponse createRoom(
            @RequestParam(required = false) UUID organizationId,
            @Valid @RequestBody SaveRoomRequest request) {
        return configurationUseCase.createRoom(organizationId, request);
    }

    @PutMapping("/rooms/{id}")
    public RoomResponse updateRoom(
            @RequestParam(required = false) UUID organizationId,
            @PathVariable UUID id,
            @Valid @RequestBody SaveRoomRequest request) {
        return configurationUseCase.updateRoom(organizationId, id, request);
    }

    @DeleteMapping("/rooms/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteRoom(@RequestParam(required = false) UUID organizationId, @PathVariable UUID id) {
        configurationUseCase.deleteRoom(organizationId, id);
    }

    @PostMapping("/beds")
    @ResponseStatus(HttpStatus.CREATED)
    public BedResponse createBed(
            @RequestParam(required = false) UUID organizationId,
            @Valid @RequestBody SaveBedRequest request) {
        return configurationUseCase.createBed(organizationId, request);
    }

    @PutMapping("/beds/{id}")
    public BedResponse updateBed(
            @RequestParam(required = false) UUID organizationId,
            @PathVariable UUID id,
            @Valid @RequestBody SaveBedRequest request) {
        return configurationUseCase.updateBed(organizationId, id, request);
    }

    @DeleteMapping("/beds/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteBed(@RequestParam(required = false) UUID organizationId, @PathVariable UUID id) {
        configurationUseCase.deleteBed(organizationId, id);
    }
}
