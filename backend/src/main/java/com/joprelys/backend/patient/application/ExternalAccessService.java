package com.joprelys.backend.patient.application;

import com.joprelys.backend.audit.application.AuditService;
import com.joprelys.backend.auth.infrastructure.persistence.UserAccountEntity;
import com.joprelys.backend.auth.infrastructure.persistence.UserAccountRepository;
import com.joprelys.backend.clinic.infrastructure.persistence.OrganizationEntity;
import com.joprelys.backend.clinic.infrastructure.persistence.OrganizationRepository;
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
    private final OrganizationRepository organizationRepository;
    private final AuditService auditService;
    private final com.joprelys.backend.notification.application.NotificationService notificationService;

    public ExternalAccessService(ExternalAccessRequestRepository externalAccessRequestRepository,
                                 PatientRepository patientRepository,
                                 UserAccountRepository userAccountRepository,
                                 OrganizationRepository organizationRepository,
                                 AuditService auditService,
                                 com.joprelys.backend.notification.application.NotificationService notificationService) {
        this.externalAccessRequestRepository = externalAccessRequestRepository;
        this.patientRepository = patientRepository;
        this.userAccountRepository = userAccountRepository;
        this.organizationRepository = organizationRepository;
        this.auditService = auditService;
        this.notificationService = notificationService;
    }

    @Transactional
    public ExternalAccessResponse requestAccess(CreateExternalAccessRequest request) {
        UserAccountEntity actor = getCurrentUser();
        if (actor == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Non authentifié.");
        }

        PatientEntity patient = patientRepository.findByGlobalPatientNumber(request.patientDpu())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Patient non trouvé avec ce numéro DPU."));

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
        if (request.scopes() != null && !request.scopes().isBlank()) {
            newRequest.setScopes(request.scopes());
        }

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

        notificationService.sendNotification(
                patient.getId(),
                "Demande d'accès externe",
                "L'établissement " + getOrganizationName(requesterOrgId) + " demande l'accès à votre dossier pour : " + request.reason(),
                "INFO"
        );

        return ExternalAccessResponse.fromEntity(saved, getOrganizationName(requesterOrgId));
    }

    @Transactional(readOnly = true)
    public List<ExternalAccessResponse> getPatientRequests(UUID patientId) {
        return externalAccessRequestRepository.findByPatientId(patientId).stream()
                .map(r -> ExternalAccessResponse.fromEntity(r, getOrganizationName(r.getRequesterOrganizationId())))
                .toList();
    }

    @Transactional
    public ExternalAccessResponse approveRequest(UUID patientId, UUID requestId) {
        return approveRequest(patientId, requestId, null);
    }

    @Transactional
    public ExternalAccessResponse approveRequest(UUID patientId, UUID requestId, String scopes) {
        ExternalAccessRequestEntity request = externalAccessRequestRepository.findById(requestId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Demande d'accès introuvable."));

        if (!request.getPatientId().equals(patientId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Cette demande ne vous concerne pas.");
        }

        if (!"EN_ATTENTE".equals(request.getStatus())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Seule une demande en attente peut être approuvée.");
        }

        request.setStatus("APPROUVEE");
        request.setExpiresAt(Instant.now().plus(java.time.Duration.ofHours(request.getRequestedDurationHours())));
        if (scopes != null && !scopes.isBlank()) {
            request.setScopes(scopes);
        }
        ExternalAccessRequestEntity saved = externalAccessRequestRepository.save(request);

        // Récupérer l'organisation du patient (si disponible) pour l'audit log, sinon celle de la demande
        UUID actorOrgId = request.getRequesterOrganizationId();
        PatientEntity patient = patientRepository.findByIdGlobally(patientId).orElse(null);
        if (patient != null && patient.getOrganizationId() != null) {
            actorOrgId = patient.getOrganizationId();
        }

        auditService.logSuccess(
                patientId, // L'acteur de l'action est le patient
                actorOrgId,
                patientId,
                "EXTERNAL_ACCESS_REQUEST",
                saved.getId(),
                "APPROVE_EXTERNAL_ACCESS",
                "Demande d'accès externe approuvée par le patient."
        );

        // Notifier le médecin demandeur que sa demande a été approuvée
        notificationService.sendNotification(
                patientId,
                "Demande d'accès approuvée",
                "Le patient a approuvé votre demande d'accès au dossier. Vous disposez maintenant d'un accès temporaire de " + saved.getRequestedDurationHours() + " heures.",
                "SECURITY"
        );

        return ExternalAccessResponse.fromEntity(saved, getOrganizationName(saved.getRequesterOrganizationId()));
    }

    @Transactional
    public ExternalAccessResponse rejectRequest(UUID patientId, UUID requestId) {
        ExternalAccessRequestEntity request = externalAccessRequestRepository.findById(requestId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Demande d'accès introuvable."));

        if (!request.getPatientId().equals(patientId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Cette demande ne vous concerne pas.");
        }

        if (!"EN_ATTENTE".equals(request.getStatus())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Seule une demande en attente peut être rejetée.");
        }

        request.setStatus("REFUSEE");
        ExternalAccessRequestEntity saved = externalAccessRequestRepository.save(request);

        // Récupérer l'organisation du patient (si disponible) pour l'audit log, sinon celle de la demande
        UUID actorOrgId = request.getRequesterOrganizationId();
        PatientEntity patient = patientRepository.findByIdGlobally(patientId).orElse(null);
        if (patient != null && patient.getOrganizationId() != null) {
            actorOrgId = patient.getOrganizationId();
        }

        auditService.logSuccess(
                patientId, // L'acteur de l'action est le patient
                actorOrgId,
                patientId,
                "EXTERNAL_ACCESS_REQUEST",
                saved.getId(),
                "REJECT_EXTERNAL_ACCESS",
                "Demande d'accès externe rejetée par le patient."
        );

        // Notifier le médecin demandeur que sa demande a été rejetée
        notificationService.sendNotification(
                patientId,
                "Demande d'accès rejetée",
                "Le patient a rejeté votre demande d'accès au dossier.",
                "SECURITY"
        );

        return ExternalAccessResponse.fromEntity(saved, getOrganizationName(saved.getRequesterOrganizationId()));
    }

    private String getOrganizationName(UUID orgId) {
        return organizationRepository.findById(orgId)
                .map(OrganizationEntity::getName)
                .orElse("Établissement inconnu");
    }

    /**
     * STORY-1909 : Révocation d'un accès externe approuvé par le patient (FR-CONSENT-004).
     * Passe le statut de APPROUVEE à REFUSEE et journalise l'action.
     */
    @Transactional
    public ExternalAccessResponse revokeApprovedRequest(UUID patientId, UUID requestId) {
        ExternalAccessRequestEntity request = externalAccessRequestRepository.findById(requestId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Demande d'accès introuvable."));

        if (!request.getPatientId().equals(patientId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Cette demande ne vous concerne pas.");
        }

        if (!"APPROUVEE".equals(request.getStatus())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Seule une demande approuvée peut être révoquée par le patient.");
        }

        request.setStatus("REFUSEE");
        request.setExpiresAt(Instant.now()); // Expire immédiatement
        ExternalAccessRequestEntity saved = externalAccessRequestRepository.save(request);

        UUID actorOrgId = request.getRequesterOrganizationId();
        PatientEntity patient = patientRepository.findByIdGlobally(patientId).orElse(null);
        if (patient != null && patient.getOrganizationId() != null) {
            actorOrgId = patient.getOrganizationId();
        }

        auditService.logSuccess(
                patientId, actorOrgId, patientId,
                "EXTERNAL_ACCESS_REQUEST", saved.getId(),
                "REVOKE_EXTERNAL_ACCESS",
                "Accès externe révoqué par le patient (FR-CONSENT-004). Org=" + saved.getRequesterOrganizationId()
        );

        notificationService.sendNotification(
                patientId,
                "Accès externe révoqué",
                "Vous avez révoqué l'accès de l'établissement " + getOrganizationName(saved.getRequesterOrganizationId()) + " à votre dossier.",
                "SECURITY"
        );

        return ExternalAccessResponse.fromEntity(saved, getOrganizationName(saved.getRequesterOrganizationId()));
    }

    private UserAccountEntity getCurrentUser() {
        var auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.isAuthenticated() && !"anonymousUser".equals(auth.getName())) {
            return userAccountRepository.findByEmail(auth.getName().trim().toLowerCase()).orElse(null);
        }
        return null;
    }
}
