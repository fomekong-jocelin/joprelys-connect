package com.joprelys.backend.patient.api;

import com.joprelys.backend.consultation.infrastructure.persistence.ConsultationRepository;
import com.joprelys.backend.patient.application.PatientAccessGuardService;
import com.joprelys.backend.patient.application.PatientConsentService;
import com.joprelys.backend.patient.infrastructure.persistence.PatientEntity;
import com.joprelys.backend.patient.infrastructure.persistence.PatientRepository;
import com.joprelys.backend.visit.application.DocumentService;
import com.joprelys.backend.visit.infrastructure.persistence.MedicalDocumentEntity;
import com.joprelys.backend.visit.infrastructure.persistence.MedicalDocumentRepository;
import com.joprelys.backend.audit.application.AuditService;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import com.joprelys.backend.patient.application.ExternalAccessService;
import org.springframework.web.server.ResponseStatusException;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/patient")
@PreAuthorize("hasAuthority('PATIENT_PORTAL_ACCESS')")
public class PatientPortalController {

    private final PatientRepository patientRepository;
    private final ConsultationRepository consultationRepository;
    private final MedicalDocumentRepository medicalDocumentRepository;
    private final DocumentService documentService;
    private final com.joprelys.backend.clinic.infrastructure.persistence.OrganizationRepository organizationRepository;
    private final com.joprelys.backend.patient.infrastructure.persistence.PatientConsentRepository patientConsentRepository;
    private final AuditService auditService;
    private final com.joprelys.backend.prescription.infrastructure.persistence.PrescriptionRepository prescriptionRepository;
    private final PatientAccessGuardService patientAccessGuardService;
    private final ExternalAccessService externalAccessService;
    private final com.joprelys.backend.notification.application.NotificationService notificationService;
    private final com.joprelys.backend.prescription.application.PrescriptionService prescriptionService;
    private final com.joprelys.backend.patient.application.PatientSummaryService patientSummaryService;
    private final com.joprelys.backend.lab.application.LabResultService labResultService;
    // STORY-1909: Service CDC de gestion des consentements
    private final PatientConsentService patientConsentService;

    public PatientPortalController(
            PatientRepository patientRepository,
            ConsultationRepository consultationRepository,
            MedicalDocumentRepository medicalDocumentRepository,
            DocumentService documentService,
            com.joprelys.backend.clinic.infrastructure.persistence.OrganizationRepository organizationRepository,
            com.joprelys.backend.patient.infrastructure.persistence.PatientConsentRepository patientConsentRepository,
            AuditService auditService,
            com.joprelys.backend.prescription.infrastructure.persistence.PrescriptionRepository prescriptionRepository,
            PatientAccessGuardService patientAccessGuardService,
            ExternalAccessService externalAccessService,
            com.joprelys.backend.notification.application.NotificationService notificationService,
            com.joprelys.backend.prescription.application.PrescriptionService prescriptionService,
            com.joprelys.backend.patient.application.PatientSummaryService patientSummaryService,
            com.joprelys.backend.lab.application.LabResultService labResultService,
            PatientConsentService patientConsentService) {
        this.patientRepository = patientRepository;
        this.consultationRepository = consultationRepository;
        this.medicalDocumentRepository = medicalDocumentRepository;
        this.documentService = documentService;
        this.organizationRepository = organizationRepository;
        this.patientConsentRepository = patientConsentRepository;
        this.auditService = auditService;
        this.prescriptionRepository = prescriptionRepository;
        this.patientAccessGuardService = patientAccessGuardService;
        this.externalAccessService = externalAccessService;
        this.notificationService = notificationService;
        this.prescriptionService = prescriptionService;
        this.patientSummaryService = patientSummaryService;
        this.labResultService = labResultService;
        this.patientConsentService = patientConsentService;
    }

    @GetMapping("/me")
    public PatientPortalMeResponse getMe(Authentication authentication) {
        PatientEntity patient = patientAccessGuardService.resolve(authentication);

        List<PatientPortalMeResponse.PatientPortalConsultation> consultations = consultationRepository
                .findByPatientIdOrderByCreatedAtDesc(patient.getId())
                .stream()
                .map(c -> {
                    MedicalDocumentEntity doc = medicalDocumentRepository.findByVisitId(c.getVisit().getId()).orElse(null);
                    LocalDate visitDate = LocalDate.ofInstant(c.getVisit().getCreatedAt(), ZoneId.systemDefault());
                    var vitals = com.joprelys.backend.visit.api.VitalsResponse.fromEntity(c.getVisit().getVitals());
                    var prescriptionOpt = prescriptionRepository.findByConsultationId(c.getId());
                    var prescriptionItems = prescriptionOpt
                            .map(p -> p.getItems().stream()
                                    .map(com.joprelys.backend.prescription.api.PrescriptionItemResponse::fromEntity)
                                    .toList())
                            .orElse(List.of());

                    UUID prescriptionId = prescriptionOpt.map(p -> p.getId()).orElse(null);
                    String prescriptionNumber = prescriptionOpt.map(p -> p.getPrescriptionNumber()).orElse(null);
                    String prescriptionStatus = prescriptionOpt.map(p -> p.getStatus()).orElse(null);
                    String prescriptionTransmissionStatus = prescriptionOpt.map(p -> p.getTransmissionStatus()).orElse(null);
                    java.time.Instant prescriptionTransmittedAt = prescriptionOpt.map(p -> p.getTransmittedAt()).orElse(null);
                    UUID prescriptionDocumentId = prescriptionOpt.map(p -> p.getDocumentId()).orElse(null);
                    String pinCode = prescriptionOpt.map(p -> p.getPinCode()).orElse(null);

                    return new PatientPortalMeResponse.PatientPortalConsultation(
                            c.getVisit().getId(),
                            c.getVisit().getVisitNumber(),
                            visitDate,
                            c.getDoctor().getDisplayName(),
                            c.getVisit().getOrientation(), // Orienté vers le service clinique comme nom de clinique/service
                            c.getDiagnosis(),
                            doc != null ? doc.getId() : null,
                            doc != null ? doc.getStatus() != null ? doc.getStatus().name() : null : null,
                            c.getSymptoms(),
                            c.getClinicalExam(),
                            c.getAdvice(),
                            c.getFollowUp(),
                            vitals,
                            prescriptionId,
                            prescriptionNumber,
                            prescriptionStatus,
                            prescriptionTransmissionStatus,
                            prescriptionTransmittedAt,
                            prescriptionDocumentId,
                            pinCode,
                            prescriptionItems
                    );
                })
                .toList();

        return new PatientPortalMeResponse(
                patient.getId(),
                patient.getGlobalPatientNumber(),
                patient.getLocalPatientNumber(),
                patient.getFullName(),
                patient.getGender(),
                patient.getBirthDate(),
                patient.getPhone(),
                patient.getCity(),
                patient.getDistrict(),
                patient.getAddress(),
                patient.getEmergencyContactName(),
                patient.getEmergencyContactPhone(),
                patient.getAllergies(),
                patient.getMedicalHistory(),
                patient.getBloodGroup(),
                patient.getEmail(),
                consultations
        );
    }

    @GetMapping("/results")
    public List<com.joprelys.backend.lab.api.LabResultResponse> getOwnResults(Authentication authentication) {
        PatientEntity patient = patientAccessGuardService.resolve(authentication);
        return labResultService.getPatientResults(patient.getId());
    }

    @GetMapping("/results/{resultId}/pdf")
    public ResponseEntity<byte[]> downloadOwnResultPdf(@PathVariable UUID resultId, Authentication authentication) {
        PatientEntity patient = patientAccessGuardService.resolve(authentication);
        var result = labResultService.getResultById(resultId);

        if (!result.patientId().equals(patient.getId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Vous n'êtes pas autorisé à accéder à ce résultat.");
        }

        byte[] pdfBytes = labResultService.getResultPdfBytes(resultId);

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"result-" + result.resultNumber() + ".pdf\"")
                .contentType(MediaType.APPLICATION_PDF)
                .body(pdfBytes);
    }

    @GetMapping("/medical-summary")
    public MedicalSummaryResponse getMedicalSummary(Authentication authentication) {
        PatientEntity patient = patientAccessGuardService.resolve(authentication);
        return patientSummaryService.getMedicalSummary(patient.getId());
    }

    @GetMapping("/summary-pdf")
    public ResponseEntity<byte[]> downloadOwnSummaryPdf(Authentication authentication) {
        PatientEntity patient = patientAccessGuardService.resolve(authentication);
        byte[] pdfBytes = patientSummaryService.generatePatientSummaryPdf(patient.getId());
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"patient-summary.pdf\"")
                .contentType(MediaType.APPLICATION_PDF)
                .body(pdfBytes);
    }

    @GetMapping("/visits/{visitId}/document")
    public ResponseEntity<byte[]> downloadOwnDocument(@PathVariable UUID visitId, Authentication authentication) {
        PatientEntity patient = patientAccessGuardService.resolve(authentication);

        // JOIN FETCH visit + patient en une seule requête → plus de LazyInitializationException
        MedicalDocumentEntity doc = medicalDocumentRepository.findByVisitIdWithVisitAndPatient(visitId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Document introuvable pour cette visite."));

        // Vérification de sécurité de l'accès au document
        if (!doc.getVisit().getPatient().getId().equals(patient.getId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Vous n'êtes pas autorisé à accéder à ce document.");
        }

        byte[] pdfBytes = documentService.loadDocumentFile(doc);

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + doc.getDocumentNumber() + ".pdf\"")
                .contentType(MediaType.APPLICATION_PDF)
                .body(pdfBytes);
    }

    @GetMapping("/documents/{documentId}/download")
    public ResponseEntity<byte[]> downloadOwnDocumentById(@PathVariable UUID documentId, Authentication authentication) {
        PatientEntity patient = patientAccessGuardService.resolve(authentication);

        MedicalDocumentEntity doc = medicalDocumentRepository.findByIdWithVisitAndPatient(documentId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Document introuvable."));

        if (!doc.getVisit().getPatient().getId().equals(patient.getId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Vous n'êtes pas autorisé à accéder à ce document.");
        }

        byte[] pdfBytes = documentService.loadDocumentFile(doc);

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + doc.getDocumentNumber() + ".pdf\"")
                .contentType(MediaType.APPLICATION_PDF)
                .body(pdfBytes);
    }

    @GetMapping("/consents")
    public List<PatientConsentDto> getConsents(Authentication authentication) {
        PatientEntity patient = patientAccessGuardService.resolve(authentication);

        return organizationRepository.findAll().stream()
                .map(org -> {
                    var consentOpt = patientConsentRepository.findByPatientIdAndOrganizationId(patient.getId(), org.getId());
                    String status;
                    String scopes;
                    String validationChannel;
                    if (consentOpt.isPresent()) {
                        status = consentOpt.get().getStatus();
                        scopes = consentOpt.get().getScopes();
                        validationChannel = consentOpt.get().getValidationChannel();
                    } else if (org.getId().equals(patient.getOrganizationId())) {
                        status = "ACTIVE";
                        scopes = "medical_records,prescriptions,lab_results,allergies_history";
                        validationChannel = "PORTAL";
                    } else {
                        status = "NONE";
                        scopes = "medical_records,prescriptions,lab_results,allergies_history";
                        validationChannel = "PORTAL";
                    }
                    boolean isCreator = org.getId().equals(patient.getOrganizationId());
                    return new PatientConsentDto(org.getId(), org.getName(), status, isCreator, scopes, validationChannel);
                })
                .toList();
    }

    @org.springframework.web.bind.annotation.PostMapping("/consents/{orgId}")
    public void updateConsent(
            @PathVariable UUID orgId,
            @org.springframework.web.bind.annotation.RequestParam String status,
            @org.springframework.web.bind.annotation.RequestParam(required = false) String scopes,
            @org.springframework.web.bind.annotation.RequestParam(required = false) String validationChannel,
            Authentication authentication) {
        if (!"ACTIVE".equals(status) && !"REVOKED".equals(status)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Statut invalide.");
        }

        PatientEntity patient = patientAccessGuardService.resolve(authentication);

        var consent = patientConsentRepository.findByPatientIdAndOrganizationId(patient.getId(), orgId)
                .orElseGet(() -> new com.joprelys.backend.patient.infrastructure.persistence.PatientConsentEntity(patient.getId(), orgId, status));

        consent.setStatus(status);
        if (scopes != null) {
            consent.setScopes(scopes);
        }
        if (validationChannel != null) {
            consent.setValidationChannel(validationChannel);
        } else {
            consent.setValidationChannel("PORTAL");
        }
        patientConsentRepository.save(consent);
    }

    // ─── STORY-1909 : Endpoints CDC consentements complets ────────────────────

    /** Historique complet des consentements du patient (tous types et statuts CDC). */
    @GetMapping("/consents/history")
    public List<ConsentResponse> getConsentHistory(Authentication authentication) {
        PatientEntity patient = patientAccessGuardService.resolve(authentication);
        return patientConsentService.getConsentHistory(patient.getId());
    }

    /** FR-CONSENT-004 : Révocation d'un consentement approuvé (APPROVED → REVOKED). */
    @PostMapping("/consents/{consentId}/revoke")
    public ConsentResponse revokeConsent(@PathVariable UUID consentId, Authentication authentication) {
        PatientEntity patient = patientAccessGuardService.resolve(authentication);
        return patientConsentService.revokeConsent(patient.getId(), consentId);
    }

    /** Approbation d'un consentement en attente (REQUESTED → APPROVED). */
    @PostMapping("/consents/{consentId}/approve")
    public ConsentResponse approveConsent(@PathVariable UUID consentId, Authentication authentication) {
        PatientEntity patient = patientAccessGuardService.resolve(authentication);
        return patientConsentService.approveConsent(patient.getId(), consentId);
    }

    /** Rejet d'un consentement en attente (REQUESTED → REJECTED). */
    @PostMapping("/consents/{consentId}/reject")
    public ConsentResponse rejectConsent(@PathVariable UUID consentId, Authentication authentication) {
        PatientEntity patient = patientAccessGuardService.resolve(authentication);
        return patientConsentService.rejectConsent(patient.getId(), consentId);
    }

    // ─── STORY-1909 : Révocation d'un accès externe approuvé ─────────────────

    /**
     * Le patient révoque un accès externe déjà approuvé (status APPROUVEE → REFUSEE).
     * Journalise l'action (FR-CONSENT-005).
     */
    @PostMapping("/access-requests/{id}/revoke")
    public ExternalAccessResponse revokeApprovedAccessRequest(
            @PathVariable UUID id, Authentication authentication) {
        PatientEntity patient = patientAccessGuardService.resolve(authentication);
        return externalAccessService.revokeApprovedRequest(patient.getId(), id);
    }

    @GetMapping("/audit-logs")
    public List<PatientAuditLogDto> getAuditLogs(Authentication authentication) {
        PatientEntity patient = patientAccessGuardService.resolve(authentication);

        return auditService.getPatientLogs(patient.getId()).stream()
                .map(log -> new PatientAuditLogDto(
                        log.getId(),
                        log.getAction(),
                        log.getReason(),
                        log.getIpAddress(),
                        log.getUserAgent(),
                        log.getStatus(),
                        log.getCreatedAt(),
                        log.getActorOrganizationId() != null ? organizationRepository.findById(log.getActorOrganizationId())
                                .map(org -> org.getName())
                                .orElse("Établissement inconnu") : "N/A"
                ))
                .toList();
    }

    @GetMapping("/access-requests")
    public List<ExternalAccessResponse> getAccessRequests(Authentication authentication) {
        PatientEntity patient = patientAccessGuardService.resolve(authentication);
        return externalAccessService.getPatientRequests(patient.getId());
    }

    @org.springframework.web.bind.annotation.PostMapping("/access-requests/{id}/approve")
    public ExternalAccessResponse approveAccessRequest(
            @PathVariable UUID id,
            @RequestParam(required = false) String scopes,
            Authentication authentication) {
        PatientEntity patient = patientAccessGuardService.resolve(authentication);
        return externalAccessService.approveRequest(patient.getId(), id, scopes);
    }

    @org.springframework.web.bind.annotation.PostMapping("/access-requests/{id}/reject")
    public ExternalAccessResponse rejectAccessRequest(@PathVariable UUID id, Authentication authentication) {
        PatientEntity patient = patientAccessGuardService.resolve(authentication);
        return externalAccessService.rejectRequest(patient.getId(), id);
    }

    @GetMapping("/notifications")
    public List<com.joprelys.backend.notification.infrastructure.persistence.NotificationEntity> getNotifications(Authentication authentication) {
        PatientEntity patient = patientAccessGuardService.resolve(authentication);
        return notificationService.getPatientNotifications(patient.getId());
    }

    @org.springframework.web.bind.annotation.PostMapping("/notifications/{id}/read")
    public com.joprelys.backend.notification.infrastructure.persistence.NotificationEntity markNotificationAsRead(
            @PathVariable UUID id, Authentication authentication) {
        PatientEntity patient = patientAccessGuardService.resolve(authentication);
        return notificationService.markAsRead(patient.getId(), id);
    }

    @org.springframework.web.bind.annotation.PostMapping("/notifications/read-all")
    public void markAllNotificationsAsRead(Authentication authentication) {
        PatientEntity patient = patientAccessGuardService.resolve(authentication);
        notificationService.markAllAsRead(patient.getId());
    }

    @org.springframework.web.bind.annotation.PostMapping("/me/prescriptions/{id}/transmit")
    public com.joprelys.backend.prescription.api.PrescriptionResponse transmitOwnPrescription(
            @PathVariable UUID id, Authentication authentication) {
        return com.joprelys.backend.prescription.api.PrescriptionResponse.fromEntity(
                prescriptionService.transmitPrescriptionForPatient(id, authentication.getName())
        );
    }
}

record PatientConsentDto(UUID organizationId, String organizationName, String status, boolean isCreator, String scopes, String validationChannel) {}

record PatientAuditLogDto(
        UUID id,
        String action,
        String reason,
        String ipAddress,
        String userAgent,
        String status,
        java.time.Instant createdAt,
        String organizationName
) {}
