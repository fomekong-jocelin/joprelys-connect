package com.joprelys.backend.patient.api;

import com.joprelys.backend.consultation.infrastructure.persistence.ConsultationRepository;
import com.joprelys.backend.patient.application.PatientAccessGuardService;
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
@PreAuthorize("hasRole('PATIENT')")
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
            ExternalAccessService externalAccessService) {
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
                    var prescriptionItems = prescriptionRepository.findByConsultationId(c.getId())
                            .map(p -> p.getItems().stream()
                                    .map(com.joprelys.backend.prescription.api.PrescriptionItemResponse::fromEntity)
                                    .toList())
                            .orElse(List.of());

                    return new PatientPortalMeResponse.PatientPortalConsultation(
                            c.getVisit().getId(),
                            c.getVisit().getVisitNumber(),
                            visitDate,
                            c.getDoctor().getDisplayName(),
                            c.getVisit().getOrientation(), // Orienté vers le service clinique comme nom de clinique/service
                            c.getDiagnosis(),
                            doc != null ? doc.getId() : null,
                            doc != null ? doc.getStatus() : null,
                            c.getSymptoms(),
                            c.getClinicalExam(),
                            c.getAdvice(),
                            c.getFollowUp(),
                            vitals,
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
                consultations
        );
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

    @GetMapping("/consents")
    public List<PatientConsentDto> getConsents(Authentication authentication) {
        PatientEntity patient = patientAccessGuardService.resolve(authentication);

        return organizationRepository.findAll().stream()
                .map(org -> {
                    var consentOpt = patientConsentRepository.findByPatientIdAndOrganizationId(patient.getId(), org.getId());
                    String status;
                    if (consentOpt.isPresent()) {
                        status = consentOpt.get().getStatus();
                    } else if (org.getId().equals(patient.getOrganizationId())) {
                        status = "ACTIVE";
                    } else {
                        status = "NONE";
                    }
                    boolean isCreator = org.getId().equals(patient.getOrganizationId());
                    return new PatientConsentDto(org.getId(), org.getName(), status, isCreator);
                })
                .toList();
    }

    @org.springframework.web.bind.annotation.PostMapping("/consents/{orgId}")
    public void updateConsent(
            @PathVariable UUID orgId,
            @org.springframework.web.bind.annotation.RequestParam String status,
            Authentication authentication) {
        if (!"ACTIVE".equals(status) && !"REVOKED".equals(status)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Statut invalide.");
        }

        PatientEntity patient = patientAccessGuardService.resolve(authentication);

        var consent = patientConsentRepository.findByPatientIdAndOrganizationId(patient.getId(), orgId)
                .orElseGet(() -> new com.joprelys.backend.patient.infrastructure.persistence.PatientConsentEntity(patient.getId(), orgId, status));

        consent.setStatus(status);
        patientConsentRepository.save(consent);
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
    public ExternalAccessResponse approveAccessRequest(@PathVariable UUID id, Authentication authentication) {
        PatientEntity patient = patientAccessGuardService.resolve(authentication);
        return externalAccessService.approveRequest(patient.getId(), id);
    }

    @org.springframework.web.bind.annotation.PostMapping("/access-requests/{id}/reject")
    public ExternalAccessResponse rejectAccessRequest(@PathVariable UUID id, Authentication authentication) {
        PatientEntity patient = patientAccessGuardService.resolve(authentication);
        return externalAccessService.rejectRequest(patient.getId(), id);
    }
}

record PatientConsentDto(UUID organizationId, String organizationName, String status, boolean isCreator) {}

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
