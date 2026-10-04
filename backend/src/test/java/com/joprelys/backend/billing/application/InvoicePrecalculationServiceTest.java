package com.joprelys.backend.billing.application;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.joprelys.backend.billing.infrastructure.persistence.InsuranceConventionRepository;
import com.joprelys.backend.consultation.infrastructure.persistence.ConsultationEntity;
import com.joprelys.backend.consultation.infrastructure.persistence.ConsultationRepository;
import com.joprelys.backend.hospitalization.infrastructure.persistence.HospitalizationDailyCareRepository;
import com.joprelys.backend.hospitalization.infrastructure.persistence.HospitalizationEntity;
import com.joprelys.backend.hospitalization.infrastructure.persistence.HospitalizationRepository;
import com.joprelys.backend.hospitalization.infrastructure.persistence.OperatingReportRepository;
import com.joprelys.backend.hospitalization.infrastructure.persistence.PatientConsumptionRepository;
import com.joprelys.backend.prescription.infrastructure.persistence.PrescriptionRepository;
import com.joprelys.backend.spatial.infrastructure.persistence.InpatientSpaceProfileRepository;
import com.joprelys.backend.visit.infrastructure.persistence.VisitEntity;
import com.joprelys.backend.visit.infrastructure.persistence.VisitRepository;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.dao.DataAccessResourceFailureException;

class InvoicePrecalculationServiceTest {
    @ParameterizedTest
    @ValueSource(strings = {"care", "consumption", "report", "prescription"})
    void aFailedClinicalSourceCannotProduceAPartialInvoice(String source) {
        var tariffs = mock(ConventionTariffService.class);
        var visits = mock(VisitRepository.class);
        var stays = mock(HospitalizationRepository.class);
        var cares = mock(HospitalizationDailyCareRepository.class);
        var consumptions = mock(PatientConsumptionRepository.class);
        var reports = mock(OperatingReportRepository.class);
        var consultations = mock(ConsultationRepository.class);
        var prescriptions = mock(PrescriptionRepository.class);
        var profiles = mock(InpatientSpaceProfileRepository.class);
        var service = new InvoicePrecalculationService(tariffs, mock(InsuranceConventionRepository.class),
                visits, stays, cares, consumptions, reports, consultations, prescriptions, profiles);
        UUID patientId = UUID.randomUUID(), visitId = UUID.randomUUID(), stayId = UUID.randomUUID();
        var visit = mock(VisitEntity.class);
        var stay = mock(HospitalizationEntity.class);
        when(visits.findById(visitId)).thenReturn(Optional.of(visit));
        when(stays.findByVisitId(visitId)).thenReturn(Optional.of(stay));
        when(stay.getId()).thenReturn(stayId);
        when(stay.getAdmittedAt()).thenReturn(Instant.now());
        when(tariffs.getTariff(anyString(), any())).thenAnswer(call -> call.getArgument(1));
        var failure = new DataAccessResourceFailureException("Clinical source unavailable");
        switch (source) {
            case "care" -> when(cares.findByHospitalizationIdOrderByPerformedAtDesc(stayId)).thenThrow(failure);
            case "consumption" -> when(consumptions.findByHospitalizationIdOrderByConsumedAtDesc(stayId)).thenThrow(failure);
            case "report" -> when(reports.findByHospitalizationIdOrderByOperationDateDesc(stayId)).thenThrow(failure);
            case "prescription" -> {
                var consultation = mock(ConsultationEntity.class);
                UUID consultationId = UUID.randomUUID();
                when(consultation.getId()).thenReturn(consultationId);
                when(consultations.findByVisitId(visitId)).thenReturn(Optional.of(consultation));
                when(prescriptions.findByConsultationId(consultationId)).thenThrow(failure);
            }
            default -> throw new IllegalArgumentException(source);
        }
        assertThrows(DataAccessResourceFailureException.class, () -> service.precalculate(patientId, visitId, null));
    }
}
