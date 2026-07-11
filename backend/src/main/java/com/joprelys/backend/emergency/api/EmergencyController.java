package com.joprelys.backend.emergency.api;

import com.joprelys.backend.emergency.application.EmergencyService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
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

    public EmergencyController(EmergencyService emergencyService) {
        this.emergencyService = emergencyService;
    }

    @PostMapping
    @PreAuthorize("hasAuthority('EMERGENCY_WRITE') or " + LEGACY_WRITE_ROLES)
    @Operation(summary = "Enregistrer une urgence", description = "Ouvre un dossier d'urgence critique pour un patient.", responses = {
            @ApiResponse(responseCode = "200", description = "Dossier d'urgence ouvert avec succès")
    })
    public EmergencyResponse create(
            @Valid @RequestBody CreateEmergencyRequest request,
            Authentication authentication) {
        var claims = (com.joprelys.backend.auth.security.JwtClaims) authentication.getDetails();
        UUID userId = UUID.fromString(claims.subject());
        return EmergencyResponse.fromEntity(emergencyService.createEmergency(request, userId));
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
    @Operation(summary = "Récupérer un dossier d'urgence", description = "Retourne les détails et l'historique de réanimation d'un dossier d'urgence.", responses = {
            @ApiResponse(responseCode = "200", description = "Dossier d'urgence trouvé"),
            @ApiResponse(responseCode = "404", description = "Introuvable")
    })
    public EmergencyResponse getById(
            @Parameter(description = "Identifiant du dossier d'urgence") @PathVariable UUID id) {
        return EmergencyResponse.fromEntity(emergencyService.getEmergency(id));
    }

    @PostMapping("/{id}/resuscitation")
    @PreAuthorize("hasAuthority('EMERGENCY_WRITE') or " + LEGACY_WRITE_ROLES)
    @Operation(summary = "Ajouter un soin de réanimation", description = "Enregistre une action de soins horodatée (bolus, voie, médicament) sur le patient.", responses = {
            @ApiResponse(responseCode = "200", description = "Soin enregistré avec succès"),
            @ApiResponse(responseCode = "404", description = "Introuvable")
    })
    public ResuscitationLogResponse addResuscitationLog(
            @Parameter(description = "Identifiant du dossier d'urgence") @PathVariable UUID id,
            @Valid @RequestBody AddResuscitationLogRequest request,
            Authentication authentication) {
        var claims = (com.joprelys.backend.auth.security.JwtClaims) authentication.getDetails();
        UUID userId = UUID.fromString(claims.subject());
        return ResuscitationLogResponse.fromEntity(emergencyService.addResuscitationLog(id, request, userId));
    }

    @PostMapping("/{id}/stabilize")
    @PreAuthorize("hasAuthority('EMERGENCY_STABILIZE') or " + LEGACY_STABILIZE_ROLES)
    @Operation(summary = "Stabiliser le patient", description = "Marque la fin des soins d'urgences, stabilise les constantes et oriente le patient (bloc, hospitalisation, sortie).", responses = {
            @ApiResponse(responseCode = "200", description = "Patient marqué comme stabilisé avec succès"),
            @ApiResponse(responseCode = "404", description = "Introuvable")
    })
    public EmergencyResponse stabilize(
            @Parameter(description = "Identifiant du dossier d'urgence") @PathVariable UUID id,
            @RequestParam String orientation) {
        return EmergencyResponse.fromEntity(emergencyService.stabilizeEmergency(id, orientation));
    }

    @GetMapping("/patient/{patientId}")
    @PreAuthorize("hasAuthority('EMERGENCY_READ') or " + LEGACY_READ_ROLES)
    @Operation(summary = "Lister les dossiers d'urgences d'un patient", description = "Retourne l'historique complet des dossiers d'urgences (y compris stabilisés) d'un patient.", responses = {
            @ApiResponse(responseCode = "200", description = "Historique récupéré avec succès")
    })
    public List<EmergencyResponse> getPatientEmergencies(@PathVariable UUID patientId) {
        return emergencyService.getPatientEmergencies(patientId).stream()
                .map(EmergencyResponse::fromEntity)
                .toList();
    }
}
