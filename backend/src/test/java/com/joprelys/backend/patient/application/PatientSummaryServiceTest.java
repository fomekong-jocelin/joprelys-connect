package com.joprelys.backend.patient.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.joprelys.backend.audit.application.AuditService;
import com.joprelys.backend.auth.infrastructure.persistence.UserAccountEntity;
import com.joprelys.backend.auth.infrastructure.persistence.UserAccountRepository;
import com.joprelys.backend.clinic.infrastructure.persistence.OrganizationRepository;
import com.joprelys.backend.common.application.VerificationUrlProvider;
import com.joprelys.backend.consultation.infrastructure.persistence.ConsultationEntity;
import com.joprelys.backend.consultation.infrastructure.persistence.ConsultationRepository;
import com.joprelys.backend.hospitalization.infrastructure.persistence.HospitalizationRepository;
import com.joprelys.backend.lab.infrastructure.persistence.LabResultEntity;
import com.joprelys.backend.lab.infrastructure.persistence.LabResultRepository;
import com.joprelys.backend.lab.infrastructure.persistence.LabResultStatus;
import com.joprelys.backend.patient.api.MedicalSummaryResponse;
import com.joprelys.backend.patient.infrastructure.persistence.PatientAllergyRepository;
import com.joprelys.backend.patient.infrastructure.persistence.PatientEntity;
import com.joprelys.backend.patient.infrastructure.persistence.PatientMedicalHistoryRepository;
import com.joprelys.backend.patient.infrastructure.persistence.PatientRepository;
import com.joprelys.backend.prescription.infrastructure.persistence.PrescriptionRepository;
import com.joprelys.backend.visit.application.PdfGeneratorService;
import com.joprelys.backend.visit.application.QrCodeGeneratorService;
import com.joprelys.backend.visit.infrastructure.persistence.VisitEntity;
import com.joprelys.backend.visit.infrastructure.persistence.VisitRepository;
import com.joprelys.backend.visit.infrastructure.persistence.VitalsRepository;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class PatientSummaryServiceTest {

    private final UUID patientId = UUID.randomUUID();
    private final PatientRepository patientRepository = mock(PatientRepository.class);
    private final ConsultationRepository consultationRepository = mock(ConsultationRepository.class);
    private final LabResultRepository labResultRepository = mock(LabResultRepository.class);

    private final PatientSummaryService service = new PatientSummaryService(
            patientRepository,
            mock(PatientAllergyRepository.class),
            mock(PatientMedicalHistoryRepository.class),
            mock(PrescriptionRepository.class),
            mock(VisitRepository.class),
            consultationRepository,
            labResultRepository,
            mock(PatientAccessPolicyService.class),
            mock(AuditService.class),
            mock(UserAccountRepository.class),
            mock(HospitalizationRepository.class),
            mock(OrganizationRepository.class),
            mock(VitalsRepository.class),
            mock(PdfGeneratorService.class),
            mock(QrCodeGeneratorService.class),
            mock(VerificationUrlProvider.class));

    @Test
    void summaryShowsOnlySignedConsultations() {
        givenPatient();
        List<ConsultationEntity> consultations = List.of(
                consultation("BROUILLON", "Diagnostic provisoire"),
                consultation("VALIDEE", "Paludisme simple"));
        when(consultationRepository.findByPatientIdOrderByCreatedAtDesc(patientId)).thenReturn(consultations);

        MedicalSummaryResponse summary = service.getMedicalSummary(patientId);

        assertThat(summary.recentDiagnostics())
                .extracting(MedicalSummaryResponse.DiagnosticSummaryDto::diagnosis)
                .containsExactly("Paludisme simple");
    }

    @Test
    void criticalResultsExcludeDraftsAndValuesCorrectedToNormal() {
        givenPatient();
        List<LabResultEntity> results = List.of(
                labResult("RES-1", "Potassium", 2, "NORMAL", LabResultStatus.VALIDATED),
                labResult("RES-1", "Potassium", 1, "CRITICAL", LabResultStatus.VALIDATED),
                labResult("RES-2", "Troponine", 1, "CRITICAL", LabResultStatus.DRAFT),
                labResult("RES-3", "Glycémie", 1, "CRITICAL", LabResultStatus.VALIDATED));
        when(labResultRepository.findByPatientIdOrderByCreatedAtDesc(patientId)).thenReturn(results);

        MedicalSummaryResponse summary = service.getMedicalSummary(patientId);

        assertThat(summary.criticalResults())
                .extracting(MedicalSummaryResponse.CriticalResultSummaryDto::analyteName)
                .containsExactly("Glycémie");
    }

    private void givenPatient() {
        PatientEntity patient = mock(PatientEntity.class);
        when(patient.getId()).thenReturn(patientId);
        when(patientRepository.findByIdGlobally(patientId)).thenReturn(Optional.of(patient));
    }

    private static ConsultationEntity consultation(String status, String diagnosis) {
        ConsultationEntity consultation = mock(ConsultationEntity.class);
        VisitEntity visit = mock(VisitEntity.class);
        UserAccountEntity doctor = mock(UserAccountEntity.class);
        when(consultation.getStatus()).thenReturn(status);
        when(consultation.getDiagnosis()).thenReturn(diagnosis);
        when(consultation.getVisit()).thenReturn(visit);
        when(consultation.getDoctor()).thenReturn(doctor);
        when(consultation.getCreatedAt()).thenReturn(Instant.now());
        return consultation;
    }

    private static LabResultEntity labResult(
            String resultNumber, String analyte, int version, String interpretation, LabResultStatus status) {
        LabResultEntity result = mock(LabResultEntity.class);
        when(result.getResultNumber()).thenReturn(resultNumber);
        when(result.getAnalyteName()).thenReturn(analyte);
        when(result.getVersion()).thenReturn(version);
        when(result.getInterpretation()).thenReturn(interpretation);
        when(result.getStatus()).thenReturn(status);
        return result;
    }
}
