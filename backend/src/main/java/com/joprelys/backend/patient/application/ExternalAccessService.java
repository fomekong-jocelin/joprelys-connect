package com.joprelys.backend.patient.application;

import com.joprelys.backend.audit.application.AuditService;
import com.joprelys.backend.auth.infrastructure.persistence.UserAccountEntity;
import com.joprelys.backend.auth.infrastructure.persistence.UserAccountRepository;
import com.joprelys.backend.patient.api.CreateExternalAccessRequest;
import com.joprelys.backend.patient.api.ExternalAccessResponse;
import com.joprelys.backend.patient.infrastructure.persistence.ExternalAccessRequestEntity;
import com.joprelys.backend.patient.infrastructure.persistence.ExternalAccessRequestRepository;
import com.joprelys.backend.patient.infrastructure.persistence.PatientEntity;
import com.joprelys.backend.patient.infrastructure.persistence.PatientRepository;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
public class ExternalAccessService {

    private final ExternalAccessRequestRepository externalAccessRequestRepository;
    private final PatientRepository patientRepository;
    private final UserAccountRepository userAccountRepository;
    private final AuditService auditService;

    public ExternalAccessService(ExternalAccessRequestRepository externalAccessRequestRepository,
                                 PatientRepository patientRepository,
                                 UserAccountRepository userAccountRepository,
                                 AuditService auditService) {
        this.externalAccessRequestRepository = externalAccessRequestRepository;
        this.patientRepository = patientRepository;
        this.userAccountRepository = userAccountRepository;
        this.auditService = auditService;
    }

    @Transactional
    public ExternalAccessResponse requestAccess(CreateExternalAccessRequest request) {
        UserAccountEntity actor = getCurrentUser();
        if (actor == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Non authentifié.");
        }

        PatientEntity patient = patientRepository.findByIdGlobally(request.patientId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Patient non trouvé."));

        UUID requesterOrgId = actor.getOrganizationId();

        // 1. Un établissement ne peut pas demander l'accès externe pour ses propres patients
        if (patient.getOrganizationId() != null && patient.getOrganizationId().equals(requesterOrgId)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Le patient appartient déjà à votre établissement.");
        }

        // 2. Vérifier s'il y a déjà une demande en cours (EN_ATTENTE ou APPROUVEE non expirée)
        List<ExternalAccessRequestEntity> existingRequests = externalAccessRequestRepository.findByPatientId(patient.getId());
        boolean hasActiveRequest = existingRequests.stream()
                .anyMatch(r -> "EN_ATTENTE".equals(r.getStatus()) ||
                        ("APPROUVEE".equals(r.getStatus()) && r.getExpiresAt() != null && r.getExpiresAt().isAfter(Instant.now())));

        if (hasActiveRequest) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Une demande d'accès est déjà active ou en attente pour ce patient.");
        }

        ExternalAccessRequestEntity newRequest = new ExternalAccessRequestEntity(
                patient.getId(),
                actor.getId(),
                requesterOrgId,
                request.reason(),
                request.durationHours()
        );

        ExternalAccessRequestEntity saved = externalAccessRequestRepository.save(newRequest);

        auditService.logSuccess(
                actor.getId(),
                requesterOrgId,
                patient.getId(),
                "EXTERNAL_ACCESS_REQUEST",
                saved.getId(),
                "REQUEST_EXTERNAL_ACCESS",
                "Demande d'accès externe créée pour le patient : " + patient.getFullName() + " (Motif: " + saved.getReason() + ")"
        );

        return ExternalAccessResponse.fromEntity(saved);
    }

    private UserAccountEntity getCurrentUser() {
        var auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.isAuthenticated() && !"anonymousUser".equals(auth.getName())) {
            return userAccountRepository.findByEmail(auth.getName().trim().toLowerCase()).orElse(null);
        }
        return null;
    }
}
