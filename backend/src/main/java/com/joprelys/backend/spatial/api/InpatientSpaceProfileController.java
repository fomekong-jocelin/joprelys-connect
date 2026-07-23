package com.joprelys.backend.spatial.api;

import com.joprelys.backend.spatial.api.HospitalLocationDtos.InpatientProfileResponse;
import com.joprelys.backend.spatial.api.HospitalLocationDtos.SaveInpatientProfileRequest;
import com.joprelys.backend.spatial.application.InpatientSpaceProfileService;
import jakarta.validation.Valid;
import java.util.UUID;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/spatial/configuration/spaces")
@PreAuthorize("hasAuthority('SPATIAL_CONFIGURATION_MANAGE')")
public class InpatientSpaceProfileController {

    private final InpatientSpaceProfileService service;

    public InpatientSpaceProfileController(InpatientSpaceProfileService service) {
        this.service = service;
    }

    @GetMapping("/{id}/inpatient-profile")
    public InpatientProfileResponse getProfile(
            @RequestParam(required = false) UUID organizationId,
            @PathVariable UUID id) {
        return service.getProfile(organizationId, id);
    }

    @PutMapping("/{id}/inpatient-profile")
    public InpatientProfileResponse saveProfile(
            @RequestParam(required = false) UUID organizationId,
            @PathVariable UUID id,
            @Valid @RequestBody SaveInpatientProfileRequest request) {
        return service.saveProfile(organizationId, id, request);
    }
}
