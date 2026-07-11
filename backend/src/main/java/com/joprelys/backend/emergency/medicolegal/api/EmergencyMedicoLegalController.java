package com.joprelys.backend.emergency.medicolegal.api;

import com.joprelys.backend.auth.security.JwtClaims;
import com.joprelys.backend.emergency.medicolegal.application.EmergencyMedicoLegalService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/emergencies/{emergencyId}/medico-legal")
@Tag(name = "Urgences — traçabilité médico-légale")
public class EmergencyMedicoLegalController {

    private static final String READ_ACCESS =
            "hasAuthority('EMERGENCY_MEDICO_LEGAL_READ') or "
                    + "hasAnyRole('INFIRMIER', 'MEDECIN', 'ADMIN_CLINIQUE', 'ADMIN_JOPRELYS', 'SUPER_ADMIN')";
    private static final String CLINICAL_WRITE_ACCESS =
            "hasAuthority('EMERGENCY_MEDICO_LEGAL_WRITE') or "
                    + "hasAnyRole('MEDECIN', 'ADMIN_CLINIQUE', 'ADMIN_JOPRELYS', 'SUPER_ADMIN')";
    private static final String CUSTODY_WRITE_ACCESS =
            "hasAuthority('EMERGENCY_BELONGINGS_WRITE') or "
                    + "hasAnyRole('INFIRMIER', 'MEDECIN', 'ADMIN_CLINIQUE', 'ADMIN_JOPRELYS', 'SUPER_ADMIN')";

    private final EmergencyMedicoLegalService service;

    public EmergencyMedicoLegalController(EmergencyMedicoLegalService service) {
        this.service = service;
    }

    @GetMapping
    @PreAuthorize(READ_ACCESS)
    @Operation(summary = "Consulter la traçabilité médico-légale d'une urgence")
    public EmergencyMedicoLegalResponse getDossier(@PathVariable UUID emergencyId) {
        return service.getDossier(emergencyId);
    }

    @PostMapping("/third-parties")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize(CUSTODY_WRITE_ACCESS)
    @Operation(summary = "Ajouter un accompagnant, déclarant ou témoin")
    public EmergencyMedicoLegalResponse addThirdParty(
            @PathVariable UUID emergencyId,
            @Valid @RequestBody CreateEmergencyThirdPartyRequest request,
            Authentication authentication) {
        return service.addThirdParty(emergencyId, request, actorId(authentication));
    }

    @PostMapping("/capacity-events")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize(CLINICAL_WRITE_ACCESS)
    @Operation(summary = "Tracer l'incapacité ou la reprise de capacité du patient")
    public EmergencyMedicoLegalResponse recordCapacity(
            @PathVariable UUID emergencyId,
            @Valid @RequestBody RecordEmergencyCapacityRequest request,
            Authentication authentication) {
        return service.recordCapacity(emergencyId, request, actorId(authentication));
    }

    @PostMapping("/legal-bases")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize(CLINICAL_WRITE_ACCESS)
    @Operation(summary = "Documenter la base légale de la prise en charge urgente")
    public EmergencyMedicoLegalResponse addLegalBasis(
            @PathVariable UUID emergencyId,
            @Valid @RequestBody CreateEmergencyLegalBasisRequest request,
            Authentication authentication) {
        return service.addLegalBasis(emergencyId, request, actorId(authentication));
    }

    @PostMapping("/belongings")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize(CUSTODY_WRITE_ACCESS)
    @Operation(summary = "Inventorier un effet personnel")
    public EmergencyMedicoLegalResponse addBelonging(
            @PathVariable UUID emergencyId,
            @Valid @RequestBody CreateEmergencyBelongingRequest request,
            Authentication authentication) {
        return service.addBelonging(emergencyId, request, actorId(authentication));
    }

    @PostMapping("/belongings/{belongingId}/transfers")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize(CUSTODY_WRITE_ACCESS)
    @Operation(summary = "Tracer une remise ou un transfert d'effet personnel")
    public EmergencyMedicoLegalResponse transferBelonging(
            @PathVariable UUID emergencyId,
            @PathVariable UUID belongingId,
            @Valid @RequestBody TransferEmergencyBelongingRequest request,
            Authentication authentication) {
        return service.transferBelonging(
                emergencyId,
                belongingId,
                request,
                actorId(authentication));
    }

    private UUID actorId(Authentication authentication) {
        JwtClaims claims = (JwtClaims) authentication.getDetails();
        return UUID.fromString(claims.subject());
    }
}
