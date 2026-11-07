package com.joprelys.backend.eco.api;

import com.joprelys.backend.eco.application.LinkedAppView;
import com.joprelys.backend.eco.application.PatientConsentRevocationUseCase;
import com.joprelys.backend.eco.application.PatientLinkedAppsQuery;
import com.joprelys.backend.eco.domain.ConsentScope;
import com.joprelys.backend.patient.application.PatientAccessGuardService;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Portail patient Joprelys : applications de l'écosystème reliées au dossier (S7).
 * Le patient vient toujours du jeton du portail, jamais d'un paramètre.
 */
@RestController
@RequestMapping("/api/patient/linked-apps")
@PreAuthorize("hasAuthority('PATIENT_PORTAL_ACCESS')")
public class PatientLinkedAppsController {

    private final PatientAccessGuardService accessGuard;
    private final PatientLinkedAppsQuery linkedApps;
    private final PatientConsentRevocationUseCase revocation;

    public PatientLinkedAppsController(PatientAccessGuardService accessGuard, PatientLinkedAppsQuery linkedApps,
                                       PatientConsentRevocationUseCase revocation) {
        this.accessGuard = accessGuard;
        this.linkedApps = linkedApps;
        this.revocation = revocation;
    }

    @GetMapping
    public List<LinkedAppResponse> list(Authentication authentication) {
        UUID patientId = accessGuard.resolve(authentication).getId();
        return linkedApps.activeLinks(patientId).stream().map(LinkedAppResponse::from).toList();
    }

    @DeleteMapping("/{linkId}")
    public ResponseEntity<Void> revoke(@PathVariable UUID linkId, Authentication authentication) {
        UUID patientId = accessGuard.resolve(authentication).getId();
        revocation.revokeByPatient(patientId, linkId);
        return ResponseEntity.noContent().build();
    }

    /** Vue portail : codes d'application et de périmètres en valeurs de contrat ; aucune donnée d'identité. */
    public record LinkedAppResponse(UUID id, String application, List<String> scopes, Instant linkedAt,
                                    Instant validUntil) {

        static LinkedAppResponse from(LinkedAppView view) {
            List<String> scopes = view.scopes().stream().sorted().map(ConsentScope::contractValue).toList();
            return new LinkedAppResponse(view.id(), view.application(), scopes, view.linkedAt(), view.validUntil());
        }
    }
}
