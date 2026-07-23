package com.joprelys.backend.hospitalorganization.api;

import com.joprelys.backend.hospitalorganization.application.HospitalOrganizationUseCase;
import jakarta.validation.Valid;
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
@RequestMapping("/api/hospital-organization")
@PreAuthorize("hasAuthority('ORGANIZATION_STRUCTURE_MANAGE')")
public class HospitalOrganizationController {

    private final HospitalOrganizationUseCase useCase;

    public HospitalOrganizationController(HospitalOrganizationUseCase useCase) {
        this.useCase = useCase;
    }

    @GetMapping("/catalogs/services")
    public List<HospitalServiceCatalogResponse> listServiceCatalog() {
        return useCase.listServiceCatalog();
    }

    @GetMapping("/catalogs/specialties")
    public List<MedicalSpecialtyCatalogResponse> listSpecialtyCatalog() {
        return useCase.listSpecialtyCatalog();
    }

    @GetMapping("/units")
    public List<OrganizationalUnitResponse> listUnits(
            @RequestParam(required = false) UUID organizationId,
            @RequestParam(defaultValue = "false") boolean includeInactive) {
        return useCase.listUnits(organizationId, includeInactive);
    }

    @PostMapping("/units")
    @ResponseStatus(HttpStatus.CREATED)
    public OrganizationalUnitResponse createUnit(
            @RequestParam(required = false) UUID organizationId,
            @Valid @RequestBody SaveOrganizationalUnitRequest request) {
        return useCase.createUnit(organizationId, request);
    }

    @PutMapping("/units/{unitId}")
    public OrganizationalUnitResponse updateUnit(
            @RequestParam(required = false) UUID organizationId,
            @PathVariable UUID unitId,
            @Valid @RequestBody SaveOrganizationalUnitRequest request) {
        return useCase.updateUnit(organizationId, unitId, request);
    }

    @PostMapping("/units/{unitId}/activate")
    public OrganizationalUnitResponse activateUnit(
            @RequestParam(required = false) UUID organizationId,
            @PathVariable UUID unitId) {
        return useCase.setUnitActive(organizationId, unitId, true);
    }

    @PostMapping("/units/{unitId}/deactivate")
    public OrganizationalUnitResponse deactivateUnit(
            @RequestParam(required = false) UUID organizationId,
            @PathVariable UUID unitId) {
        return useCase.setUnitActive(organizationId, unitId, false);
    }
}
