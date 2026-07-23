package com.joprelys.backend.patient.application;

import com.joprelys.backend.audit.application.AuditService;
import com.joprelys.backend.auth.infrastructure.persistence.UserAccountEntity;
import com.joprelys.backend.auth.infrastructure.persistence.UserAccountRepository;
import com.joprelys.backend.consultation.infrastructure.persistence.ConsultationEntity;
import com.joprelys.backend.consultation.infrastructure.persistence.ConsultationRepository;
import com.joprelys.backend.lab.infrastructure.persistence.LabResultEntity;
import com.joprelys.backend.lab.infrastructure.persistence.LabResultRepository;
import com.joprelys.backend.patient.api.MedicalSummaryResponse;
import com.joprelys.backend.patient.infrastructure.persistence.PatientAllergyRepository;
import com.joprelys.backend.patient.infrastructure.persistence.PatientEntity;
import com.joprelys.backend.patient.infrastructure.persistence.PatientMedicalHistoryRepository;
import com.joprelys.backend.patient.infrastructure.persistence.PatientRepository;
import com.joprelys.backend.prescription.infrastructure.persistence.PrescriptionEntity;
import com.joprelys.backend.prescription.infrastructure.persistence.PrescriptionRepository;
import com.joprelys.backend.visit.infrastructure.persistence.VisitEntity;
import com.joprelys.backend.visit.infrastructure.persistence.VisitRepository;
import com.joprelys.backend.common.application.VerificationUrlProvider;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

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
    private final com.joprelys.backend.hospitalization.infrastructure.persistence.HospitalizationRepository hospitalizationRepository;
    private final com.joprelys.backend.clinic.infrastructure.persistence.OrganizationRepository organizationRepository;
    private final com.joprelys.backend.visit.infrastructure.persistence.VitalsRepository vitalsRepository;
    private final com.joprelys.backend.visit.application.PdfGeneratorService pdfGeneratorService;
    private final com.joprelys.backend.visit.application.QrCodeGeneratorService qrCodeGeneratorService;
    private final VerificationUrlProvider verificationUrlProvider;

    public PatientSummaryService(
            PatientRepository patientRepository,
            PatientAllergyRepository patientAllergyRepository,
            PatientMedicalHistoryRepository patientMedicalHistoryRepository,
            PrescriptionRepository prescriptionRepository,
            VisitRepository visitRepository,
            ConsultationRepository consultationRepository,
            LabResultRepository labResultRepository,
            PatientService patientService,
            AuditService auditService,
            UserAccountRepository userAccountRepository,
            com.joprelys.backend.hospitalization.infrastructure.persistence.HospitalizationRepository hospitalizationRepository,
            com.joprelys.backend.clinic.infrastructure.persistence.OrganizationRepository organizationRepository,
            com.joprelys.backend.visit.infrastructure.persistence.VitalsRepository vitalsRepository,
            com.joprelys.backend.visit.application.PdfGeneratorService pdfGeneratorService,
            com.joprelys.backend.visit.application.QrCodeGeneratorService qrCodeGeneratorService,
            VerificationUrlProvider verificationUrlProvider) {
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
        this.hospitalizationRepository = hospitalizationRepository;
        this.organizationRepository = organizationRepository;
        this.vitalsRepository = vitalsRepository;
        this.pdfGeneratorService = pdfGeneratorService;
        this.qrCodeGeneratorService = qrCodeGeneratorService;
        this.verificationUrlProvider = verificationUrlProvider;
    }

    @Transactional(readOnly = true)
    public MedicalSummaryResponse getMedicalSummary(UUID patientId) {
        patientService.validateAccess(patientId, "medical_records");

        PatientEntity patient = patientRepository.findByIdGlobally(patientId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Patient non trouvé"));

        List<MedicalSummaryResponse.AllergySummaryDto> allergies = patientAllergyRepository
                .findAllByPatientIdAndDeletedAtIsNull(patientId).stream()
                .filter(allergy -> "ACTIVE".equalsIgnoreCase(allergy.getStatus()))
                .map(allergy -> new MedicalSummaryResponse.AllergySummaryDto(
                        allergy.getId(),
                        allergy.getSubstance(),
                        allergy.getSeverity(),
                        allergy.getReaction(),
                        allergy.getStatus(),
                        allergy.getDiscoveredAt()))
                .collect(Collectors.toList());

        List<MedicalSummaryResponse.MedicalHistorySummaryDto> histories = patientMedicalHistoryRepository
                .findAllByPatientIdAndDeletedAtIsNull(patientId).stream()
                .filter(history -> history.isOngoing() || history.isImportant())
                .map(history -> new MedicalSummaryResponse.MedicalHistorySummaryDto(
                        history.getId(),
                        history.getCategory(),
                        history.getDescription(),
                        history.getOnsetDate(),
                        history.isOngoing(),
                        history.isImportant(),
                        history.getComment()))
                .collect(Collectors.toList());

        List<PrescriptionEntity> activePrescriptionsList = prescriptionRepository
                .findActivePrescriptionsByPatientId(patientId);
        List<MedicalSummaryResponse.TreatmentSummaryDto> activePrescriptions = activePrescriptionsList.stream()
                .map(prescription -> new MedicalSummaryResponse.TreatmentSummaryDto(
                        prescription.getId(),
                        prescription.getPrescriptionNumber(),
                        prescription.getStatus(),
                        prescription.getCreatedAt(),
                        prescription.getItems().stream()
                                .map(item -> new MedicalSummaryResponse.TreatmentSummaryDto.PrescriptionItemDto(
                                        item.getDrugName(),
                                        item.getDosage(),
                                        item.getPosology(),
                                        item.getDuration(),
                                        item.getQuantity(),
                                        item.getInstructions()))
                                .collect(Collectors.toList())))
                .collect(Collectors.toList());

        List<VisitEntity> visits = visitRepository.findByPatientId(patientId).stream()
                .sorted((left, right) -> right.getCreatedAt().compareTo(left.getCreatedAt()))
                .limit(3)
                .collect(Collectors.toList());
        List<MedicalSummaryResponse.VisitSummaryDto> recentVisits = visits.stream()
                .map(visit -> new MedicalSummaryResponse.VisitSummaryDto(
                        visit.getId(),
                        visit.getVisitNumber(),
                        visit.getReason(),
                        visit.getOrientation(),
                        visit.getService(),
                        visit.getCreatedAt()))
                .collect(Collectors.toList());

        List<ConsultationEntity> consultations = consultationRepository
                .findByPatientIdOrderByCreatedAtDesc(patientId).stream()
                .limit(3)
                .collect(Collectors.toList());
        List<MedicalSummaryResponse.DiagnosticSummaryDto> recentDiagnostics = consultations.stream()
                .map(consultation -> new MedicalSummaryResponse.DiagnosticSummaryDto(
                        consultation.getId(),
                        consultation.getVisit().getVisitNumber(),
                        consultation.getCreatedAt(),
                        consultation.getDoctor().getDisplayName(),
                        consultation.getSuspectedDiagnosis(),
                        consultation.getDiagnosis(),
                        consultation.getFinalDiagnosis(),
                        consultation.getConclusion()))
                .collect(Collectors.toList());

        List<LabResultEntity> criticalResultsList = labResultRepository
                .findByPatientIdAndInterpretationOrderByCreatedAtDesc(patientId, "CRITICAL");
        List<MedicalSummaryResponse.CriticalResultSummaryDto> criticalResults = criticalResultsList.stream()
                .map(result -> new MedicalSummaryResponse.CriticalResultSummaryDto(
                        result.getId(),
                        result.getResultNumber(),
                        result.getAnalyteName(),
                        result.getValue(),
                        result.getUnit(),
                        result.getReferenceRange(),
                        result.getInterpretation(),
                        result.getValidatorName(),
                        result.getValidatedAt()))
                .collect(Collectors.toList());

        List<MedicalSummaryResponse.HospitalizationSummaryDto> hospitalizations = hospitalizationRepository
                .findByPatientIdOrderByAdmittedAtDesc(patientId).stream()
                .map(hospitalization -> new MedicalSummaryResponse.HospitalizationSummaryDto(
                        hospitalization.getId(),
                        hospitalization.getHospitalizationNumber(),
                        hospitalization.getServiceName(),
                        hospitalization.getSpaceName(),
                        hospitalization.getBedNumber(),
                        hospitalization.getAdmissionReason(),
                        hospitalization.getStatus(),
                        hospitalization.getAdmittedAt(),
                        hospitalization.getDischargedAt(),
                        hospitalization.getDischargeDiagnosis(),
                        hospitalization.getDischargeInstructions()))
                .collect(Collectors.toList());

        UserAccountEntity actor = getCurrentUser();
        if (actor != null) {
            auditService.logSuccess(
                    actor.getId(),
                    actor.getOrganizationId(),
                    patientId,
                    "PATIENT_RECORD",
                    patientId,
                    "READ_PATIENT_SUMMARY",
                    "Lecture de la synthèse médicale pour le patient : " + patient.getFullName());
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
                criticalResults,
                hospitalizations);
    }

    @Transactional(readOnly = true)
    public byte[] generatePatientSummaryPdf(UUID patientId) {
        MedicalSummaryResponse summary = getMedicalSummary(patientId);

        UUID orgId = patientRepository.findById(patientId)
                .map(PatientEntity::getOrganizationId)
                .orElse(null);
        var orgOpt = orgId != null
                ? organizationRepository.findById(orgId)
                : java.util.Optional.<com.joprelys.backend.clinic.infrastructure.persistence.OrganizationEntity>empty();
        String orgName = orgOpt.map(o -> o.getName()).orElse("Clinique Joprelys");
        String orgAddress = orgOpt.map(o -> o.getAddress()).orElse("");
        String orgPhone = orgOpt.map(o -> o.getPhone()).orElse("");

        var vitalsList = vitalsRepository.findAllByPatientId(patientId);
        com.joprelys.backend.visit.infrastructure.persistence.VitalsEntity vitals = vitalsList.isEmpty()
                ? null
                : vitalsList.get(0);

        String verificationUrl = verificationUrlProvider.getVerificationUrl("patient-summary/" + patientId);
        byte[] qrCodeBytes = qrCodeGeneratorService.generateQrCode(verificationUrl, 200, 200);
        byte[] pdfBytes = pdfGeneratorService.generatePatientSummaryPdf(
                summary,
                vitals,
                orgName,
                orgAddress,
                orgPhone,
                qrCodeBytes);

        var actor = getCurrentUser();
        if (actor != null) {
            auditService.logSuccess(
                    actor.getId(),
                    actor.getOrganizationId(),
                    patientId,
                    "PATIENT_RECORD",
                    patientId,
                    "DOWNLOAD_SUMMARY_PDF",
                    "Téléchargement du PDF de synthèse médicale pour le patient : " + summary.fullName());
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
