package com.joprelys.backend.ai.application;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.joprelys.backend.patient.domain.IdentityConfidenceLevel;
import com.joprelys.backend.patient.domain.PatientIdentityStatus;
import com.joprelys.backend.patient.infrastructure.persistence.PatientEntity;
import com.joprelys.backend.prescription.infrastructure.persistence.PrescriptionEntity;
import com.joprelys.backend.prescription.infrastructure.persistence.PrescriptionItemEntity;
import com.joprelys.backend.prescription.infrastructure.persistence.PrescriptionRepository;
import com.joprelys.backend.visit.application.VisitService;
import com.joprelys.backend.visit.infrastructure.persistence.VitalsEntity;
import com.joprelys.backend.visit.infrastructure.persistence.VisitEntity;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class ClinicalContextAssemblerTest {

    @Test
    @SuppressWarnings("unchecked")
    void shouldIncludeClinicalSafetyContextAndExcludeAdministrativePii() {
        UUID visitId = UUID.randomUUID();
        UUID patientId = UUID.randomUUID();
        VisitService visitService = mock(VisitService.class);
        PrescriptionRepository prescriptionRepository = mock(PrescriptionRepository.class);
        VisitEntity visit = mock(VisitEntity.class);
        PatientEntity patient = mock(PatientEntity.class);
        VitalsEntity vitals = mock(VitalsEntity.class);
        PrescriptionEntity prescription = mock(PrescriptionEntity.class);
        PrescriptionItemEntity item = mock(PrescriptionItemEntity.class);

        when(visitService.getVisit(visitId)).thenReturn(visit);
        when(visit.getPatient()).thenReturn(patient);
        when(visit.getVitals()).thenReturn(vitals);
        when(visit.getReason()).thenReturn("Fièvre et céphalées");
        when(visit.getService()).thenReturn("Médecine générale");
        when(patient.getId()).thenReturn(patientId);
        when(patient.getFullName()).thenReturn("Nom qui ne doit pas sortir");
        when(patient.getPhone()).thenReturn("+237600000000");
        when(patient.getAddress()).thenReturn("Adresse privée");
        when(patient.getGender()).thenReturn("MASCULIN");
        when(patient.getBirthDate()).thenReturn(LocalDate.now().minusYears(42));
        when(patient.getAllergies()).thenReturn("Pénicilline");
        when(patient.getMedicalHistory()).thenReturn("HTA");
        when(patient.getBloodGroup()).thenReturn("O+");
        when(patient.getIdentityStatus()).thenReturn(PatientIdentityStatus.VERIFIED);
        when(patient.getIdentityConfidenceLevel()).thenReturn(IdentityConfidenceLevel.VERIFIED);
        when(vitals.getTemperature()).thenReturn(new BigDecimal("38.4"));
        when(vitals.getSystolic()).thenReturn(132);
        when(vitals.getDiastolic()).thenReturn(84);
        when(vitals.getSpo2()).thenReturn(96);
        when(prescriptionRepository.findActivePrescriptionsByPatientId(patientId))
                .thenReturn(List.of(prescription));
        when(prescription.getItems()).thenReturn(List.of(item));
        when(item.getDrugName()).thenReturn("Amlodipine");
        when(item.getDosage()).thenReturn("5 mg");
        when(item.getFrequency()).thenReturn("1 fois par jour");

        Map<String, Object> context = new ClinicalContextAssembler(
                visitService, prescriptionRepository).assemble(visitId);

        Map<String, Object> patientContext = (Map<String, Object>) context.get("patient");
        Map<String, Object> vitalContext = (Map<String, Object>) context.get("vitals");
        List<Map<String, Object>> medications =
                (List<Map<String, Object>>) context.get("activeMedications");

        assertEquals(42, patientContext.get("ageYears"));
        assertEquals("Pénicilline", patientContext.get("allergies"));
        assertEquals("HTA", patientContext.get("medicalHistory"));
        assertEquals(new BigDecimal("38.4"), vitalContext.get("temperatureC"));
        assertEquals(132, vitalContext.get("systolicMmHg"));
        assertEquals("Amlodipine", medications.getFirst().get("drugName"));

        String serialized = context.toString();
        assertFalse(serialized.contains("Nom qui ne doit pas sortir"));
        assertFalse(serialized.contains("+237600000000"));
        assertFalse(serialized.contains("Adresse privée"));
        assertFalse(serialized.contains("globalPatientNumber"));
        assertTrue(context.containsKey("contextPolicy"));
    }

    @Test
    @SuppressWarnings("unchecked")
    void shouldUseEstimatedDemographicsForProvisionalEmergencyIdentity() {
        UUID visitId = UUID.randomUUID();
        UUID patientId = UUID.randomUUID();
        VisitService visitService = mock(VisitService.class);
        PrescriptionRepository prescriptionRepository = mock(PrescriptionRepository.class);
        VisitEntity visit = mock(VisitEntity.class);
        PatientEntity patient = mock(PatientEntity.class);

        when(visitService.getVisit(visitId)).thenReturn(visit);
        when(visit.getPatient()).thenReturn(patient);
        when(patient.getId()).thenReturn(patientId);
        when(patient.getApparentGender()).thenReturn("MASCULIN");
        when(patient.getEstimatedAgeRange()).thenReturn("40-50 ans");
        when(patient.getIdentityStatus()).thenReturn(PatientIdentityStatus.PROVISIONAL_URGENCY);
        when(patient.getIdentityConfidenceLevel()).thenReturn(IdentityConfidenceLevel.LOW);
        when(prescriptionRepository.findActivePrescriptionsByPatientId(patientId))
                .thenReturn(List.of());

        Map<String, Object> context = new ClinicalContextAssembler(
                visitService, prescriptionRepository).assemble(visitId);
        Map<String, Object> patientContext = (Map<String, Object>) context.get("patient");

        assertEquals("MASCULIN", patientContext.get("gender"));
        assertEquals("40-50 ans", patientContext.get("estimatedAgeRange"));
        assertEquals("PROVISIONAL_URGENCY", patientContext.get("identityStatus"));
        assertEquals("LOW", patientContext.get("identityConfidence"));
        assertFalse(patientContext.containsKey("ageYears"));
    }
}
