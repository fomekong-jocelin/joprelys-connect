package com.joprelys.backend.emergency.triage.api;

import com.joprelys.backend.auth.security.JwtClaims;
import com.joprelys.backend.emergency.triage.application.EmergencyTriageAssessmentUseCase;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.net.URI;
import java.util.List;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/emergencies/{emergencyId}/triage-assessments")
@Tag(name = "Urgences — triage ABCDE")
public class EmergencyTriageAssessmentController {

    private static final String READ_ACCESS = "hasAuthority('EMERGENCY_READ')";
    private static final String WRITE_ACCESS = "hasAuthority('EMERGENCY_WRITE')";

    private final EmergencyTriageAssessmentUseCase useCase;

    public EmergencyTriageAssessmentController(EmergencyTriageAssessmentUseCase useCase) {
        this.useCase = useCase;
    }

    @GetMapping
    @PreAuthorize(READ_ACCESS)
    @Operation(summary = "Consulter l'historique du triage ABCDE")
    public List<EmergencyTriageAssessmentResponse> getHistory(
            @PathVariable UUID emergencyId,
            Authentication authentication) {
        return useCase.getHistory(emergencyId, actorId(authentication)).stream()
                .map(EmergencyTriageAssessmentApiMapper::toResponse)
                .toList();
    }

    @GetMapping("/{assessmentId}")
    @PreAuthorize(READ_ACCESS)
    @Operation(summary = "Consulter une évaluation de triage ABCDE")
    public EmergencyTriageAssessmentResponse getAssessment(
            @PathVariable UUID emergencyId,
            @PathVariable UUID assessmentId,
            Authentication authentication) {
        return EmergencyTriageAssessmentApiMapper.toResponse(
                useCase.getAssessment(emergencyId, assessmentId, actorId(authentication)));
    }

    @PostMapping
    @PreAuthorize(WRITE_ACCESS)
    @Operation(summary = "Ajouter une réévaluation ABCDE horodatée")
    public ResponseEntity<EmergencyTriageAssessmentResponse> addReassessment(
            @PathVariable UUID emergencyId,
            @Valid @RequestBody CreateEmergencyTriageAssessmentRequest request,
            Authentication authentication) {
        EmergencyTriageAssessmentResponse response = EmergencyTriageAssessmentApiMapper.toResponse(
                useCase.addReassessment(
                        emergencyId,
                        EmergencyTriageAssessmentApiMapper.fromReassessment(request),
                        actorId(authentication)));
        URI location = URI.create(
                "/api/emergencies/" + emergencyId + "/triage-assessments/" + response.id());
        return ResponseEntity.created(location).body(response);
    }

    private UUID actorId(Authentication authentication) {
        JwtClaims claims = (JwtClaims) authentication.getDetails();
        return UUID.fromString(claims.subject());
    }
}
