package com.joprelys.backend.patient.api;

import com.joprelys.backend.patient.application.ProvisionalPatientRegularizationService;
import com.joprelys.backend.patient.infrastructure.persistence.PatientEntity;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.UUID;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/patients/provisional")
@Tag(name = "Patients", description = "Gestion du dossier patient unique (DPU)")
public class ProvisionalPatientIdentityController {

    private static final String LEGACY_PATIENT_ROLES =
            "hasAnyRole('AGENT_ACCUEIL', 'INFIRMIER', 'MEDECIN', 'ADMIN_CLINIQUE')";

    private final ProvisionalPatientRegularizationService regularizationService;

    public ProvisionalPatientIdentityController(
            ProvisionalPatientRegularizationService regularizationService) {
        this.regularizationService = regularizationService;
    }

    @PutMapping("/{id}/identity")
    @PreAuthorize("hasAuthority('PATIENT_WRITE') or " + LEGACY_PATIENT_ROLES)
    @Operation(
            summary = "Identifier un patient URG-TEMP",
            description = "Régularise l'identité administrative d'un dossier provisoire sans perdre son historique d'urgence.")
    public PatientResponse regularize(
            @PathVariable UUID id,
            @Valid @RequestBody RegularizeProvisionalPatientRequest request) {
        PatientEntity entity = regularizationService.regularize(id, request);
        return map(entity);
    }

    private PatientResponse map(PatientEntity entity) {
        return new PatientResponse(
                entity.getId(),
                entity.getOrganizationId(),
                entity.getGlobalPatientNumber(),
                entity.getLocalPatientNumber(),
                entity.getDisplayName(),
                entity.getGender(),
                entity.getBirthDate(),
                entity.getPhone(),
                entity.getCity(),
                entity.getDistrict(),
                entity.getAddress(),
                entity.getEmergencyContactName(),
                entity.getEmergencyContactPhone(),
                entity.getAllergies(),
                entity.getMedicalHistory(),
                entity.getStatus(),
                entity.getBloodGroup(),
                entity.getEmail(),
                entity.getCreatedAt(),
                entity.getUpdatedAt(),
                Boolean.FALSE,
                entity.getIdentityStatus(),
                entity.getTemporaryPatientNumber(),
                entity.getDisplayName(),
                entity.getIdentityConfidenceLevel(),
                entity.getApparentGender(),
                entity.getEstimatedAgeRange(),
                entity.getPhysicalDescription(),
                entity.getFoundAt(),
                entity.getFoundLocation());
    }
}
