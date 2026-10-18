package com.joprelys.backend.patient.api;

import com.joprelys.backend.auth.security.JwtClaims;
import com.joprelys.backend.patient.application.PatientDeathService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.time.Instant;
import java.util.UUID;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/patients/{patientId}/death")
public class PatientDeathController {

    private final PatientDeathService deathService;

    public PatientDeathController(PatientDeathService deathService) {
        this.deathService = deathService;
    }

    @PostMapping
    @PreAuthorize("hasAuthority('PATIENT_DEATH_DECLARE')")
    public PatientDeathResponse declare(
            @PathVariable UUID patientId,
            @Valid @RequestBody DeclareDeathRequest request,
            Authentication authentication) {
        JwtClaims claims = (JwtClaims) authentication.getDetails();
        var patient = deathService.declareDeath(patientId,
                new PatientDeathService.DeathDeclaration(request.deceasedAt(), request.place(), request.circumstances()),
                UUID.fromString(claims.subject()), organizationId(claims));
        return PatientDeathResponse.from(patient);
    }

    @PostMapping("/revoke")
    @PreAuthorize("hasAuthority('PATIENT_DEATH_DECLARE')")
    public PatientDeathResponse revoke(
            @PathVariable UUID patientId,
            @Valid @RequestBody RevokeDeathRequest request,
            Authentication authentication) {
        JwtClaims claims = (JwtClaims) authentication.getDetails();
        var patient = deathService.revokeDeath(patientId, request.reason(),
                UUID.fromString(claims.subject()), organizationId(claims));
        return PatientDeathResponse.from(patient);
    }

    private static UUID organizationId(JwtClaims claims) {
        return claims.organizationId() == null || claims.organizationId().isBlank()
                ? null
                : UUID.fromString(claims.organizationId());
    }

    public record DeclareDeathRequest(
            Instant deceasedAt,
            @Size(max = 200, message = "Le lieu ne peut pas dépasser 200 caractères.") String place,
            @Size(max = 1000, message = "Les circonstances ne peuvent pas dépasser 1000 caractères.") String circumstances) {
    }

    public record RevokeDeathRequest(
            @NotBlank(message = "Le motif est obligatoire.")
            @Size(max = 500, message = "Le motif ne peut pas dépasser 500 caractères.") String reason) {
    }

    public record PatientDeathResponse(UUID patientId, String status, Instant deceasedAt, String place, UUID declaredBy) {
        static PatientDeathResponse from(com.joprelys.backend.patient.infrastructure.persistence.PatientEntity patient) {
            return new PatientDeathResponse(patient.getId(), patient.getStatus(), patient.getDeceasedAt(),
                    patient.getDeathPlace(), patient.getDeathDeclaredBy());
        }
    }
}
