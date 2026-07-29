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
import java.time.Instant;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

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

        if (patient.getOrganizationId() != null && patient.getOrganizationId().equals(requesterOrgId)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Le patient appartient déjà à votre établissement.");
        }

        Instant now = Instant.now();
        List<ExternalAccessRequestEntity> requesterRequests = externalAccessRequestRepository.findByPatientId(patient.getId()).stream()
                .filter(existing -> requesterOrgId.equals(existing.getRequesterOrganizationId()))
                .toList();

        boolean pendingForRequester = requesterRequests.stream()
                .anyMatch(existing -> "EN_ATTENTE".equals(existing.getStatus()));
        if (pendingForRequester) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Une demande d'accès est déjà en attente pour ce patient et votre établissement.");
        }

        Set<String> requestedScopes = parseScopes(request.scopes());
        Set<String> alreadyGrantedScopes = new LinkedHashSet<>();
        requesterRequests.stream()
                .filter(existing -> "APPROUVEE".equals(existing.getStatus()))
                .filter(existing -> existing.getExpiresAt() != null && existing.getExpiresAt().isAfter(now))
                .forEach(existing -> alreadyGrantedScopes.addAll(parseScopes(existing.getScopes())));

        if (!requestedScopes.isEmpty() && alreadyGrantedScopes.containsAll(requestedScopes)) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Les périmètres demandés sont déjà autorisés pour votre établissement.");
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
                "Demande d'accès externe créée pour le patient : " + patient.getFullName()
                        + " (Motif: " + saved.getReason() + "; Scopes: " + saved.getScopes() + ")"
        );

        notificationService.sendNotification(
                patient.getId(),
                alreadyGrantedScopes.isEmpty() ? "Demande d'accès externe" : "Demande d'extension d'accès",
                "L'établissement " + getOrganizationName(requesterOrgId)
                        + " demande l'accès à votre dossier pour : " + request.reason(),
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

        UUID actorOrgId = request.getRequesterOrganizationId();
        PatientEntity patient = patientRepository.findByIdGlobally(patientId).orElse(null);
        if (patient != null && patient.getOrganizationId() != null) {
            actorOrgId = patient.getOrganizationId();
        }

        auditService.logSuccess(
                patientId,
                actorOrgId,
                patientId,
                "EXTERNAL_ACCESS_REQUEST",
                saved.getId(),
                "APPROVE_EXTERNAL_ACCESS",
                "Demande d'accès externe approuvée par le patient."
        );

        notificationService.sendNotification(
                patientId,
                "Demande d'accès approuvée",
                "Le patient a approuvé votre demande d'accès au dossier. Vous disposez maintenant d'un accès temporaire de "
                        + saved.getRequestedDurationHours() + " heures.",
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

        UUID actorOrgId = request.getRequesterOrganizationId();
        PatientEntity patient = patientRepository.findByIdGlobally(patientId).orElse(null);
        if (patient != null && patient.getOrganizationId() != null) {
            actorOrgId = patient.getOrganizationId();
        }

        auditService.logSuccess(
                patientId,
                actorOrgId,
                patientId,
                "EXTERNAL_ACCESS_REQUEST",
                saved.getId(),
                "REJECT_EXTERNAL_ACCESS",
                "Demande d'accès externe rejetée par le patient."
        );

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
        request.setExpiresAt(Instant.now());
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

    private Set<String> parseScopes(String scopes) {
        Set<String> result = new LinkedHashSet<>();
        if (scopes == null || scopes.isBlank()) {
            return result;
        }
        for (String scope : scopes.split(",")) {
            String normalized = scope.trim().toLowerCase();
            if (!normalized.isBlank()) {
                result.add(normalized);
            }
        }
        return result;
    }

    private UserAccountEntity getCurrentUser() {
        var auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.isAuthenticated() && !"anonymousUser".equals(auth.getName())) {
            return userAccountRepository.findByEmail(auth.getName().trim().toLowerCase()).orElse(null);
        }
        return null;
    }
}
