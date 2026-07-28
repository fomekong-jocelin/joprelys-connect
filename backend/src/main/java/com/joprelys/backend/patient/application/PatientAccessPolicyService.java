package com.joprelys.backend.patient.application;

import com.joprelys.backend.auth.infrastructure.persistence.UserAccountRepository;
import com.joprelys.backend.common.api.ApiStatusException;
import com.joprelys.backend.patient.infrastructure.persistence.EmergencyAccessAuthorizationRepository;
import com.joprelys.backend.patient.infrastructure.persistence.ExternalAccessRequestRepository;
import com.joprelys.backend.patient.infrastructure.persistence.PatientConsentRepository;
import com.joprelys.backend.patient.infrastructure.persistence.PatientRepository;
import java.time.Instant;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

/**
 * Central access policy for professional access to patient sub-resources.
 *
 * <p>Important HTTP semantics:
 * - 404 means that the patient/resource really does not exist;
 * - 403 CONSENT_REQUIRED means that an access grant must be requested;
 * - 403 SCOPE_REQUIRED means that an existing grant does not cover the requested scope.
 *
 * <p>Consent and approved external-access grants are cumulative: an external grant may
 * provide a scope that is absent from the standing consent.
 */
@Service
public class PatientAccessPolicyService {

    private final PatientRepository patientRepository;
    private final UserAccountRepository userAccountRepository;
    private final PatientConsentRepository patientConsentRepository;
    private final EmergencyAccessAuthorizationRepository emergencyAccessAuthorizationRepository;
    private final ExternalAccessRequestRepository externalAccessRequestRepository;

    public PatientAccessPolicyService(
            PatientRepository patientRepository,
            UserAccountRepository userAccountRepository,
            PatientConsentRepository patientConsentRepository,
            EmergencyAccessAuthorizationRepository emergencyAccessAuthorizationRepository,
            ExternalAccessRequestRepository externalAccessRequestRepository) {
        this.patientRepository = patientRepository;
        this.userAccountRepository = userAccountRepository;
        this.patientConsentRepository = patientConsentRepository;
        this.emergencyAccessAuthorizationRepository = emergencyAccessAuthorizationRepository;
        this.externalAccessRequestRepository = externalAccessRequestRepository;
    }

    public void validateAccess(UUID patientId, String requiredScope) {
        var security = SecurityContextHolder.getContext().getAuthentication();
        if (security == null || !security.isAuthenticated() || "anonymousUser".equals(security.getName())) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Non authentifié.");
        }

        boolean patientRole = security.getAuthorities().stream()
                .anyMatch(authority -> "ROLE_PATIENT".equals(authority.getAuthority()));
        if (patientRole) {
            var currentPatient = patientRepository.findByGlobalPatientNumber(security.getName());
            if (currentPatient.isPresent() && currentPatient.get().getId().equals(patientId)) {
                return;
            }
            throw new ApiStatusException(
                    HttpStatus.FORBIDDEN,
                    "PATIENT_RECORD_ACCESS_DENIED",
                    "Vous ne pouvez accéder qu'à votre propre dossier patient.");
        }

        var actor = userAccountRepository.findByEmail(security.getName().trim().toLowerCase())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Utilisateur non authentifié."));
        UUID organizationId = actor.getOrganizationId();

        var patient = patientRepository.findByIdGlobally(patientId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Patient non trouvé."));

        if (patient.getOrganizationId() != null && patient.getOrganizationId().equals(organizationId)) {
            return;
        }

        Instant now = Instant.now();
        boolean emergencyAccess = emergencyAccessAuthorizationRepository
                .findByPatientIdAndOrganizationIdAndExpiresAtAfter(patientId, organizationId, now)
                .isPresent();
        if (emergencyAccess) {
            return;
        }

        boolean hasActiveGrant = false;

        var consent = patientConsentRepository.findByPatientIdAndOrganizationId(patientId, organizationId);
        if (consent.isPresent()) {
            var grant = consent.get();
            boolean active = "ACTIVE".equals(grant.getStatus()) || "APPROVED".equals(grant.getStatus());
            boolean notExpired = grant.getExpiresAt() == null || grant.getExpiresAt().isAfter(now);
            if (active && notExpired) {
                hasActiveGrant = true;
                if (hasScope(grant.getScopes(), requiredScope)) {
                    return;
                }
            }
        }

        var externalGrants = externalAccessRequestRepository.findByPatientId(patientId).stream()
                .filter(request -> organizationId.equals(request.getRequesterOrganizationId()))
                .filter(request -> "APPROUVEE".equals(request.getStatus()))
                .filter(request -> request.getExpiresAt() != null && request.getExpiresAt().isAfter(now))
                .toList();
        if (!externalGrants.isEmpty()) {
            hasActiveGrant = true;
            if (externalGrants.stream().anyMatch(request -> hasScope(request.getScopes(), requiredScope))) {
                return;
            }
        }

        if (hasActiveGrant) {
            throw new ApiStatusException(
                    HttpStatus.FORBIDDEN,
                    "SCOPE_REQUIRED",
                    "L'accès au patient est autorisé, mais le périmètre « " + scopeLabel(requiredScope)
                            + " » n'a pas été partagé. Demandez une extension d'accès.",
                    "REQUEST_ACCESS",
                    requiredScope);
        }

        throw new ApiStatusException(
                HttpStatus.FORBIDDEN,
                "CONSENT_REQUIRED",
                "Le patient n'a pas encore autorisé cet établissement à consulter cette partie de son dossier. Demandez son accès.",
                "REQUEST_ACCESS",
                requiredScope);
    }

    private boolean hasScope(String scopes, String requiredScope) {
        if (requiredScope == null || requiredScope.isBlank()) {
            return true;
        }
        if (scopes == null || scopes.isBlank()) {
            return false;
        }
        for (String scope : scopes.split(",")) {
            if (scope.trim().equalsIgnoreCase(requiredScope.trim())) {
                return true;
            }
        }
        return false;
    }

    private String scopeLabel(String scope) {
        if (scope == null) return "demandé";
        return switch (scope) {
            case "medical_records" -> "dossier médical";
            case "prescriptions" -> "ordonnances";
            case "lab_results" -> "résultats de laboratoire";
            case "allergies_history" -> "allergies et antécédents";
            default -> scope;
        };
    }
}
