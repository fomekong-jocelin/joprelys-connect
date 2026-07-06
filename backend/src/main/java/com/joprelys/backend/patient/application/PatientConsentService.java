package com.joprelys.backend.patient.application;

import com.joprelys.backend.audit.application.AuditService;
import com.joprelys.backend.patient.api.ConsentRequest;
import com.joprelys.backend.patient.api.ConsentResponse;
import com.joprelys.backend.patient.infrastructure.persistence.ConsentStatus;
import com.joprelys.backend.patient.infrastructure.persistence.ConsentType;
import com.joprelys.backend.patient.infrastructure.persistence.PatientConsentEntity;
import com.joprelys.backend.patient.infrastructure.persistence.PatientConsentRepository;
import com.joprelys.backend.patient.infrastructure.persistence.PatientRepository;
import org.springframework.http.HttpStatus;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Service CDC de gestion des consentements patient — STORY-1909 Module 12.
 *
 * Responsabilités :
 * - Cycle de vie complet : REQUESTED → APPROVED / REJECTED → REVOKED / EXPIRED
 * - FR-CONSENT-003 : gestion de l'expiration (expires_at)
 * - FR-CONSENT-004 : révocation explicite par le patient
 * - FR-CONSENT-005 : journalisation de chaque accès via AuditService
 */
@Service
public class PatientConsentService {

    private final PatientConsentRepository consentRepository;
    private final PatientRepository patientRepository;
    private final AuditService auditService;
    private final com.joprelys.backend.notification.application.NotificationService notificationService;

    public PatientConsentService(PatientConsentRepository consentRepository,
                                 PatientRepository patientRepository,
                                 AuditService auditService,
                                 com.joprelys.backend.notification.application.NotificationService notificationService) {
        this.consentRepository = consentRepository;
        this.patientRepository = patientRepository;
        this.auditService = auditService;
        this.notificationService = notificationService;
    }

    /**
     * Récupère l'historique complet des consentements d'un patient.
     * FR-CONSENT-005 : chaque accès est journalisé.
     */
    @Transactional(readOnly = true)
    public List<ConsentResponse> getConsentHistory(UUID patientId) {
        return consentRepository.findByPatientId(patientId)
                .stream()
                .map(ConsentResponse::fromEntity)
                .toList();
    }

    /**
     * Crée ou met à jour un consentement patient pour un établissement.
     * Rétrocompatibilité avec l'existant (type ETABLISSEMENT par défaut).
     */
    @Transactional
    public ConsentResponse createOrUpdateConsent(UUID patientId, UUID organizationId,
                                                  String status, String scopes,
                                                  String validationChannel) {
        var existing = consentRepository.findByPatientIdAndOrganizationId(patientId, organizationId);

        PatientConsentEntity consent = existing.orElseGet(() ->
                new PatientConsentEntity(patientId, organizationId, status)
        );

        consent.setStatus(status);
        if (scopes != null && !scopes.isBlank()) {
            consent.setScopes(scopes);
        }
        if (validationChannel != null && !validationChannel.isBlank()) {
            consent.setValidationChannel(validationChannel);
        } else {
            consent.setValidationChannel("PORTAL");
        }

        PatientConsentEntity saved = consentRepository.save(consent);

        // FR-CONSENT-005 : journalisation
        auditService.logSuccess(
                patientId, organizationId, patientId,
                "PATIENT_CONSENT", saved.getId(),
                "UPDATE_CONSENT",
                "Consentement mis à jour : " + status + " pour org=" + organizationId
        );

        return ConsentResponse.fromEntity(saved);
    }

    /**
     * Crée un consentement CDC complet avec type, durée et motif.
     * STORY-1909 — FR-CONSENT-003 : expires_at obligatoire selon le type.
     */
    @Transactional
    public ConsentResponse createCdcConsent(UUID patientId, ConsentRequest request,
                                             UUID requesterUserId, UUID requesterOrganizationId) {
        PatientConsentEntity consent = new PatientConsentEntity(
                patientId,
                requesterOrganizationId,
                ConsentStatus.REQUESTED.name(),
                request.consentType(),
                requesterUserId,
                requesterOrganizationId,
                request.reason(),
                request.expiresAt()
        );
        if (request.scopes() != null && !request.scopes().isBlank()) {
            consent.setScopes(request.scopes());
        }
        if (request.validationChannel() != null && !request.validationChannel().isBlank()) {
            consent.setValidationChannel(request.validationChannel());
        }

        PatientConsentEntity saved = consentRepository.save(consent);

        // Notifier le patient
        notificationService.sendNotification(
                patientId,
                "Nouvelle demande de consentement",
                "Un établissement demande votre consentement (" + request.consentType() + "). Motif : " + request.reason(),
                "INFO"
        );

        // FR-CONSENT-005 : journaliser
        auditService.logSuccess(
                requesterUserId, requesterOrganizationId, patientId,
                "PATIENT_CONSENT", saved.getId(),
                "REQUEST_CONSENT",
                "Demande de consentement CDC type=" + request.consentType()
        );

        return ConsentResponse.fromEntity(saved);
    }

    /**
     * Approuve un consentement en attente (REQUESTED → APPROVED).
     * FR-CONSENT-005 : journalisé.
     */
    @Transactional
    public ConsentResponse approveConsent(UUID patientId, UUID consentId) {
        PatientConsentEntity consent = findAndCheckOwnership(patientId, consentId);

        if (!ConsentStatus.REQUESTED.name().equals(consent.getStatus())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Seul un consentement en statut REQUESTED peut être approuvé.");
        }

        consent.setStatus(ConsentStatus.APPROVED.name());
        consent.setApprovedAt(Instant.now());
        PatientConsentEntity saved = consentRepository.save(consent);

        auditService.logSuccess(
                patientId, consent.getOrganizationId(), patientId,
                "PATIENT_CONSENT", saved.getId(),
                "APPROVE_CONSENT",
                "Consentement approuvé par le patient. Type=" + consent.getConsentType()
        );

        notificationService.sendNotification(
                patientId,
                "Consentement approuvé",
                "Vous avez approuvé un consentement d'accès à votre dossier.",
                "SECURITY"
        );

        return ConsentResponse.fromEntity(saved);
    }

    /**
     * Rejette un consentement en attente (REQUESTED → REJECTED).
     */
    @Transactional
    public ConsentResponse rejectConsent(UUID patientId, UUID consentId) {
        PatientConsentEntity consent = findAndCheckOwnership(patientId, consentId);

        if (!ConsentStatus.REQUESTED.name().equals(consent.getStatus())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Seul un consentement en statut REQUESTED peut être rejeté.");
        }

        consent.setStatus(ConsentStatus.REJECTED.name());
        PatientConsentEntity saved = consentRepository.save(consent);

        auditService.logSuccess(
                patientId, consent.getOrganizationId(), patientId,
                "PATIENT_CONSENT", saved.getId(),
                "REJECT_CONSENT",
                "Consentement rejeté par le patient."
        );

        return ConsentResponse.fromEntity(saved);
    }

    /**
     * FR-CONSENT-004 : Révoque un consentement approuvé (APPROVED → REVOKED).
     */
    @Transactional
    public ConsentResponse revokeConsent(UUID patientId, UUID consentId) {
        PatientConsentEntity consent = findAndCheckOwnership(patientId, consentId);

        if (!ConsentStatus.APPROVED.name().equals(consent.getStatus())
                && !"ACTIVE".equals(consent.getStatus())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Seul un consentement actif peut être révoqué.");
        }

        consent.setStatus(ConsentStatus.REVOKED.name());
        PatientConsentEntity saved = consentRepository.save(consent);

        // FR-CONSENT-005 : journaliser
        auditService.logSuccess(
                patientId, consent.getOrganizationId(), patientId,
                "PATIENT_CONSENT", saved.getId(),
                "REVOKE_CONSENT",
                "Consentement révoqué par le patient (FR-CONSENT-004)."
        );

        notificationService.sendNotification(
                patientId,
                "Consentement révoqué",
                "Vous avez révoqué l'accès de l'établissement à votre dossier médical.",
                "SECURITY"
        );

        return ConsentResponse.fromEntity(saved);
    }

    /**
     * FR-CONSENT-003 : planificateur d'expiration automatique.
     * Exécuté toutes les heures — marque APPROVED+expiré → EXPIRED.
     */
    @Scheduled(fixedDelay = 3_600_000)
    @Transactional
    public void expireOutdatedConsents() {
        int count = consentRepository.expireOutdatedConsents(Instant.now());
        if (count > 0) {
            System.out.println("[ConsentService] " + count + " consentement(s) expiré(s) automatiquement.");
        }
    }

    // ─── Helpers ────────────────────────────────────────────────────────────────

    private PatientConsentEntity findAndCheckOwnership(UUID patientId, UUID consentId) {
        PatientConsentEntity consent = consentRepository.findById(consentId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Consentement introuvable."));
        if (!consent.getPatientId().equals(patientId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                    "Ce consentement ne vous appartient pas.");
        }
        return consent;
    }
}
