package com.joprelys.backend.patient.application;

import com.joprelys.backend.audit.application.AuditService;
import com.joprelys.backend.auth.infrastructure.persistence.UserAccountEntity;
import com.joprelys.backend.auth.infrastructure.persistence.UserAccountRepository;
import com.joprelys.backend.consultation.infrastructure.persistence.ConsultationEntity;
import com.joprelys.backend.consultation.infrastructure.persistence.ConsultationRepository;
import com.joprelys.backend.lab.infrastructure.persistence.LabResultEntity;
import com.joprelys.backend.lab.infrastructure.persistence.LabResultRepository;
import com.joprelys.backend.patient.api.MedicalSummaryResponse;
import com.joprelys.backend.patient.infrastructure.persistence.*;
import com.joprelys.backend.prescription.infrastructure.persistence.PrescriptionEntity;
import com.joprelys.backend.prescription.infrastructure.persistence.PrescriptionRepository;
import com.joprelys.backend.visit.infrastructure.persistence.VisitEntity;
import com.joprelys.backend.visit.infrastructure.persistence.VisitRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class PatientSummaryService {

    private final PatientRepository patientRepository;
    private final PatientAllergyRepository patientAllergyRepository;
    private final PatientMedicalHistoryRepository patientMedicalHistoryRepository;
    private final PrescriptionRepository prescriptionRepository;
    private final VisitRepository visitRepository;
    private final ConsultationRepository consultationRepository;
    private final LabResultRepository labResultRepository;
    private final PatientService patientService;
    private final AuditService auditService;
    private final UserAccountRepository userAccountRepository;
    
    private final com.joprelys.backend.clinic.infrastructure.persistence.OrganizationRepository organizationRepository;
    private final com.joprelys.backend.visit.infrastructure.persistence.VitalsRepository vitalsRepository;
    private final com.joprelys.backend.visit.application.PdfGeneratorService pdfGeneratorService;
    private final com.joprelys.backend.visit.application.QrCodeGeneratorService qrCodeGeneratorService;

    @Value("${joprelys.documents.verification-base-url:http://localhost:4200/verify}")
    private String verificationBaseUrl;

    public PatientSummaryService(PatientRepository patientRepository,
                                 PatientAllergyRepository patientAllergyRepository,
                                 PatientMedicalHistoryRepository patientMedicalHistoryRepository,
                                 PrescriptionRepository prescriptionRepository,
                                 VisitRepository visitRepository,
                                 ConsultationRepository consultationRepository,
                                 LabResultRepository labResultRepository,
                                 PatientService patientService,
                                 AuditService auditService,
                                 UserAccountRepository userAccountRepository,
                                 com.joprelys.backend.clinic.infrastructure.persistence.OrganizationRepository organizationRepository,
                                 com.joprelys.backend.visit.infrastructure.persistence.VitalsRepository vitalsRepository,
                                 com.joprelys.backend.visit.application.PdfGeneratorService pdfGeneratorService,
                                 com.joprelys.backend.visit.application.QrCodeGeneratorService qrCodeGeneratorService) {
        this.patientRepository = patientRepository;
        this.patientAllergyRepository = patientAllergyRepository;
        this.patientMedicalHistoryRepository = patientMedicalHistoryRepository;
        this.prescriptionRepository = prescriptionRepository;
        this.visitRepository = visitRepository;
        this.consultationRepository = consultationRepository;
        this.labResultRepository = labResultRepository;
        this.patientService = patientService;
        this.auditService = auditService;
        this.userAccountRepository = userAccountRepository;
        this.organizationRepository = organizationRepository;
        this.vitalsRepository = vitalsRepository;
        this.pdfGeneratorService = pdfGeneratorService;
        this.qrCodeGeneratorService = qrCodeGeneratorService;
    }

    @Transactional(readOnly = true)
    public MedicalSummaryResponse getMedicalSummary(UUID patientId) {
        // Validate access controls
        patientService.validateAccess(patientId, "medical_records");

        PatientEntity patient = patientRepository.findByIdGlobally(patientId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Patient non trouvé"));

        // 1. Allergies actives structurées (status = ACTIVE)
        List<MedicalSummaryResponse.AllergySummaryDto> allergies = patientAllergyRepository
                .findAllByPatientIdAndDeletedAtIsNull(patientId).stream()
                .filter(a -> "ACTIVE".equalsIgnoreCase(a.getStatus()))
                .map(a -> new MedicalSummaryResponse.AllergySummaryDto(
                        a.getId(),
                        a.getSubstance(),
                        a.getSeverity(),
                        a.getReaction(),
                        a.getStatus(),
                        a.getDiscoveredAt()
                ))
                .collect(Collectors.toList());

        // 2. Antécédents importants ou en cours (isOngoing = true ou important = true)
        List<MedicalSummaryResponse.MedicalHistorySummaryDto> histories = patientMedicalHistoryRepository
                .findAllByPatientIdAndDeletedAtIsNull(patientId).stream()
                .filter(h -> h.isOngoing() || h.isImportant())
                .map(h -> new MedicalSummaryResponse.MedicalHistorySummaryDto(
                        h.getId(),
                        h.getCategory(),
                        h.getDescription(),
                        h.getOnsetDate(),
                        h.isOngoing(),
                        h.isImportant(),
                        h.getComment()
                ))
                .collect(Collectors.toList());

        // 3. Traitements / prescriptions en cours (prescriptions ACTIVE)
        List<PrescriptionEntity> activePrescriptionsList = prescriptionRepository
                .findActivePrescriptionsByPatientId(patientId);

        List<MedicalSummaryResponse.TreatmentSummaryDto> activePrescriptions = activePrescriptionsList.stream()
                .map(p -> new MedicalSummaryResponse.TreatmentSummaryDto(
                        p.getId(),
                        p.getPrescriptionNumber(),
                        p.getStatus(),
                        p.getCreatedAt(),
                        p.getItems().stream()
                                .map(item -> new MedicalSummaryResponse.TreatmentSummaryDto.PrescriptionItemDto(
                                        item.getDrugName(),
                                        item.getDosage(),
                                        item.getPosology(),
                                        item.getDuration(),
                                        item.getQuantity(),
                                        item.getInstructions()
                                ))
                                .collect(Collectors.toList())
                ))
                .collect(Collectors.toList());

        // 4. Dernières visites (3 dernières)
        List<VisitEntity> visits = visitRepository.findByPatientId(patientId).stream()
                .sorted((v1, v2) -> v2.getCreatedAt().compareTo(v1.getCreatedAt()))
                .limit(3)
                .collect(Collectors.toList());

        List<MedicalSummaryResponse.VisitSummaryDto> recentVisits = visits.stream()
                .map(v -> new MedicalSummaryResponse.VisitSummaryDto(
                        v.getId(),
                        v.getVisitNumber(),
                        v.getReason(),
                        v.getOrientation(),
                        v.getService(),
                        v.getCreatedAt()
                ))
                .collect(Collectors.toList());

        // 5. Derniers diagnostics (3 derniers)
        List<ConsultationEntity> consultations = consultationRepository.findByPatientIdOrderByCreatedAtDesc(patientId).stream()
                .limit(3)
                .collect(Collectors.toList());

        List<MedicalSummaryResponse.DiagnosticSummaryDto> recentDiagnostics = consultations.stream()
                .map(c -> new MedicalSummaryResponse.DiagnosticSummaryDto(
                        c.getId(),
                        c.getVisit().getVisitNumber(),
                        c.getCreatedAt(),
                        c.getDoctor().getDisplayName(),
                        c.getSuspectedDiagnosis(),
                        c.getDiagnosis(),
                        c.getFinalDiagnosis(),
                        c.getConclusion()
                ))
                .collect(Collectors.toList());

        // 6. Derniers résultats critiques (interpretation = CRITICAL)
        List<LabResultEntity> criticalResultsList = labResultRepository
                .findByPatientIdAndInterpretationOrderByCreatedAtDesc(patientId, "CRITICAL");

        List<MedicalSummaryResponse.CriticalResultSummaryDto> criticalResults = criticalResultsList.stream()
                .map(r -> new MedicalSummaryResponse.CriticalResultSummaryDto(
                        r.getId(),
                        r.getResultNumber(),
                        r.getAnalyteName(),
                        r.getValue(),
                        r.getUnit(),
                        r.getReferenceRange(),
                        r.getInterpretation(),
                        r.getValidatorName(),
                        r.getValidatedAt()
                ))
                .collect(Collectors.toList());

        // Audit Log
        UserAccountEntity actor = getCurrentUser();
        if (actor != null) {
            auditService.logSuccess(
                    actor.getId(),
                    actor.getOrganizationId(),
                    patientId,
                    "PATIENT_RECORD",
                    patientId,
                    "READ_PATIENT_SUMMARY",
                    "Lecture de la synthèse médicale pour le patient : " + patient.getFullName()
            );
        }

        return new MedicalSummaryResponse(
                patient.getId(),
                patient.getFullName(),
                patient.getGlobalPatientNumber(),
                patient.getBirthDate(),
                patient.getBloodGroup(),
                patient.getGender(),
                allergies,
                histories,
                activePrescriptions,
                recentVisits,
                recentDiagnostics,
                criticalResults
        );
    }

    @Transactional(readOnly = true)
    public byte[] generatePatientSummaryPdf(UUID patientId) {
        MedicalSummaryResponse summary = getMedicalSummary(patientId);

        // Get organization details
        var orgOpt = organizationRepository.findById(summary.patientId());
        String orgName = orgOpt.map(o -> o.getName()).orElse("Clinique Joprelys");
        String orgAddress = orgOpt.map(o -> o.getAddress()).orElse("");
        String orgPhone = orgOpt.map(o -> o.getPhone()).orElse("");

        // Get recent vitals
        var vitalsList = vitalsRepository.findAllByPatientId(patientId);
        com.joprelys.backend.visit.infrastructure.persistence.VitalsEntity vitals = vitalsList.isEmpty() ? null : vitalsList.get(0);

        // Generate QR Code pointing to verification page of patient summary
        String verificationUrl = verificationBaseUrl + "/patient-summary/" + patientId;
        byte[] qrCodeBytes = qrCodeGeneratorService.generateQrCode(verificationUrl, 200, 200);

        // Generate PDF
        byte[] pdfBytes = pdfGeneratorService.generatePatientSummaryPdf(
                summary,
                vitals,
                orgName,
                orgAddress,
                orgPhone,
                qrCodeBytes
        );

        // Log audit
        var actor = getCurrentUser();
        if (actor != null) {
            auditService.logSuccess(
                    actor.getId(),
                    actor.getOrganizationId(),
                    patientId,
                    "PATIENT_RECORD",
                    patientId,
                    "DOWNLOAD_SUMMARY_PDF",
                    "Téléchargement du PDF de synthèse médicale pour le patient : " + summary.fullName()
            );
        }

        return pdfBytes;
    }

    private UserAccountEntity getCurrentUser() {
        var auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.isAuthenticated() && !"anonymousUser".equals(auth.getName())) {
            return userAccountRepository.findByEmail(auth.getName().trim().toLowerCase()).orElse(null);
        }
        return null;
    }
}
