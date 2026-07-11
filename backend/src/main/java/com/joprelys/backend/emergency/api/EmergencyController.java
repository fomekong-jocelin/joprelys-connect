package com.joprelys.backend.emergency.api;

import com.joprelys.backend.auth.security.JwtClaims;
import com.joprelys.backend.emergency.application.EmergencyService;
import com.joprelys.backend.emergency.application.ProvisionalEmergencyAdmissionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/emergencies")
@Tag(name = "Urgences & Réanimation", description = "Gestion de l'admission et des soins d'urgences critiques")
public class EmergencyController {

    private static final String LEGACY_READ_ROLES =
            "hasAnyRole('AGENT_ACCUEIL', 'INFIRMIER', 'MEDECIN', 'ADMIN_CLINIQUE', 'ADMIN_JOPRELYS', 'SUPER_ADMIN')";
    private static final String LEGACY_WRITE_ROLES =
            "hasAnyRole('INFIRMIER', 'MEDECIN', 'ADMIN_CLINIQUE', 'ADMIN_JOPRELYS', 'SUPER_ADMIN')";
    private static final String LEGACY_STABILIZE_ROLES =
            "hasAnyRole('MEDECIN', 'ADMIN_CLINIQUE', 'ADMIN_JOPRELYS', 'SUPER_ADMIN')";

    private final EmergencyService emergencyService;
    private final ProvisionalEmergencyAdmissionService provisionalAdmissionService;

    public EmergencyController(
            EmergencyService emergencyService,
            ProvisionalEmergencyAdmissionService provisionalAdmissionService) {
        this.emergencyService = emergencyService;
        this.provisionalAdmissionService = provisionalAdmissionService;
    }

    @PostMapping
    @PreAuthorize("hasAuthority('EMERGENCY_WRITE') or " + LEGACY_WRITE_ROLES)
    @Operation(summary = "Enregistrer une urgence", description = "Ouvre un dossier d'urgence critique pour un patient.", responses = {
            @ApiResponse(responseCode = "200", description = "Dossier d'urgence ouvert avec succès")
    })
    public EmergencyResponse create(
            @Valid @RequestBody CreateEmergencyRequest request,
            Authentication authentication) {
        return EmergencyResponse.fromEntity(
                emergencyService.createEmergency(request, authenticatedUserId(authentication)));
    }

    @PostMapping("/provisional")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAuthority('EMERGENCY_WRITE') or " + LEGACY_WRITE_ROLES)
    @Operation(
            summary = "Créer une admission d'urgence URG-TEMP",
            description = "Crée de manière atomique et idempotente le patient provisoire, le dossier d'urgence et le triage initial.")
    public EmergencyResponse createProvisional(
            @Valid @RequestBody CreateProvisionalEmergencyAdmissionRequest request,
            Authentication authentication) {
        return EmergencyResponse.fromEntity(
                provisionalAdmissionService.create(request, authenticatedUserId(authentication)));
    }

    @GetMapping("/active")
    @PreAuthorize("hasAuthority('EMERGENCY_READ') or " + LEGACY_READ_ROLES)
    @Operation(summary = "Lister les urgences actives", description = "Retourne la liste des dossiers d'urgences en cours (non stabilisés).", responses = {
            @ApiResponse(responseCode = "200", description = "Liste récupérée avec succès")
    })
    public List<EmergencyResponse> getActive() {
        return emergencyService.getActiveEmergencies().stream()
                .map(EmergencyResponse::fromEntity)
                .toList();
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('EMERGENCY_READ') or " + LEGACY_READ_ROLES)
    public EmergencyResponse getById(
            @Parameter(description = "Identifiant du dossier d'urgence") @PathVariable UUID id) {
        return EmergencyResponse.fromEntity(emergencyService.getEmergency(id));
    }

    @PostMapping("/{id}/resuscitation")
    @PreAuthorize("hasAuthority('EMERGENCY_WRITE') or " + LEGACY_WRITE_ROLES)
    public ResuscitationLogResponse addResuscitationLog(
            @PathVariable UUID id,
            @Valid @RequestBody AddResuscitationLogRequest request,
            Authentication authentication) {
        return ResuscitationLogResponse.fromEntity(
                emergencyService.addResuscitationLog(id, request, authenticatedUserId(authentication)));
    }

    @PostMapping("/{id}/stabilize")
    @PreAuthorize("hasAuthority('EMERGENCY_STABILIZE') or " + LEGACY_STABILIZE_ROLES)
    public EmergencyResponse stabilize(
            @PathVariable UUID id,
            @RequestParam String orientation) {
        return EmergencyResponse.fromEntity(emergencyService.stabilizeEmergency(id, orientation));
    }

    @GetMapping("/patient/{patientId}")
    @PreAuthorize("hasAuthority('EMERGENCY_READ') or " + LEGACY_READ_ROLES)
    public List<EmergencyResponse> getPatientEmergencies(@PathVariable UUID patientId) {
        return emergencyService.getPatientEmergencies(patientId).stream()
                .map(EmergencyResponse::fromEntity)
                .toList();
    }

    private UUID authenticatedUserId(Authentication authentication) {
        var claims = (JwtClaims) authentication.getDetails();
        return UUID.fromString(claims.subject());
    }
}
