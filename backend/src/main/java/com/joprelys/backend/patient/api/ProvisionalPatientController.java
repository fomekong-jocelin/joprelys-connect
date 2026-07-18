package com.joprelys.backend.patient.api;

import com.joprelys.backend.patient.application.ProvisionalPatientService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/patients/provisional")
@Tag(name = "Patients", description = "Gestion du dossier patient unique (DPU)")
public class ProvisionalPatientController {

    private final ProvisionalPatientService provisionalPatientService;

    public ProvisionalPatientController(ProvisionalPatientService provisionalPatientService) {
        this.provisionalPatientService = provisionalPatientService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAuthority('PATIENT_WRITE')")
    @Operation(
            summary = "Créer un patient provisoire d'urgence",
            description = "Crée un dossier URG-TEMP sans exiger une identité administrative complète.")
    public ProvisionalPatientResponse create(@Valid @RequestBody CreateProvisionalPatientRequest request) {
        var result = provisionalPatientService.create(request);
        var patient = result.patient();
        var details = new ProvisionalPatientDetailsResponse(
                patient.getId(),
                patient.getOrganizationId(),
                patient.getGlobalPatientNumber(),
                patient.getLocalPatientNumber(),
                patient.getIdentityStatus(),
                patient.getTemporaryPatientNumber(),
                patient.getDisplayName(),
                patient.getIdentityConfidenceLevel(),
                patient.getApparentGender(),
                patient.getEstimatedAgeRange(),
                patient.getPhysicalDescription(),
                patient.getFoundAt(),
                patient.getFoundLocation(),
                patient.getCreatedAt(),
                patient.getUpdatedAt());

        var declarations = result.declarations().stream()
                .map(declaration -> new IdentityDeclarationResponse(
                        declaration.getId(),
                        declaration.getFieldName(),
                        declaration.getDeclaredValue(),
                        declaration.getSourceType(),
                        declaration.getSourceDetails(),
                        declaration.getConfidenceLevel(),
                        declaration.getVerificationStatus(),
                        declaration.getDeclaredBy(),
                        declaration.getDeclaredAt()))
                .toList();

        return new ProvisionalPatientResponse(details, declarations);
    }
}
