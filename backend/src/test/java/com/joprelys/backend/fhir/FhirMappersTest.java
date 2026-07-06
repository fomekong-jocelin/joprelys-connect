package com.joprelys.backend.fhir;

import com.joprelys.backend.fhir.model.*;
import com.joprelys.backend.patient.infrastructure.persistence.PatientEntity;
import com.joprelys.backend.visit.infrastructure.persistence.VisitEntity;
import com.joprelys.backend.visit.infrastructure.persistence.VitalsEntity;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class FhirMappersTest {

    private static void setField(Object target, String fieldName, Object value) {
        try {
            java.lang.reflect.Field field = target.getClass().getDeclaredField(fieldName);
            field.setAccessible(true);
            field.set(target, value);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Test
    void testPatientMapper_FullDetails() {
        PatientEntity patient = new PatientEntity(
                "DPU-12345",
                "PAT-Local",
                "Jean-Paul Belmondo",
                "MASCULIN",
                LocalDate.of(1933, 4, 9),
                "+33102030405",
                "Paris",
                "IDF",
                "123 Rue de Rivoli",
                "Alice",
                "+33600000000",
                "Pollen",
                "None"
        );

        FhirPatientDto fhirPatient = FhirPatientMapper.toFhir(patient);

        assertThat(fhirPatient).isNotNull();
        assertThat(fhirPatient.resourceType()).isEqualTo("Patient");
        assertThat(fhirPatient.gender()).isEqualTo("male");
        assertThat(fhirPatient.birthDate()).isEqualTo("1933-04-09");
        
        // Identifiers
        assertThat(fhirPatient.identifier()).hasSize(1);
        assertThat(fhirPatient.identifier().get(0).system()).isEqualTo("urn:oid:1.3.6.1.4.1.59367.1.1");
        assertThat(fhirPatient.identifier().get(0).value()).isEqualTo("DPU-12345");

        // Telecom
        assertThat(fhirPatient.telecom()).hasSize(1);
        assertThat(fhirPatient.telecom().get(0).system()).isEqualTo("phone");
        assertThat(fhirPatient.telecom().get(0).value()).isEqualTo("+33102030405");

        // Name
        assertThat(fhirPatient.name()).hasSize(1);
        HumanName name = fhirPatient.name().get(0);
        assertThat(name.use()).isEqualTo("official");
        assertThat(name.text()).isEqualTo("Jean-Paul Belmondo");
        assertThat(name.family()).isEqualTo("Belmondo");
        assertThat(name.given()).containsExactly("Jean-Paul");
    }

    @Test
    void testPatientMapper_SpecialNamesAndGenders() {
        // Single word name
        PatientEntity patient1 = new PatientEntity("D-1", "P-1", "Jocelin", "FEMININ", LocalDate.of(2000, 1, 1), "", "", "", "", "", "", "", "");
        FhirPatientDto fhirPatient1 = FhirPatientMapper.toFhir(patient1);
        assertThat(fhirPatient1.name().get(0).family()).isEqualTo("Jocelin");
        assertThat(fhirPatient1.name().get(0).given()).isEmpty();
        assertThat(fhirPatient1.gender()).isEqualTo("female");

        // Unknown gender
        PatientEntity patient2 = new PatientEntity("D-2", "P-2", "Test", "AUTRE", LocalDate.of(2000, 1, 1), "", "", "", "", "", "", "", "");
        FhirPatientDto fhirPatient2 = FhirPatientMapper.toFhir(patient2);
        assertThat(fhirPatient2.gender()).isEqualTo("other");

        // Null gender
        PatientEntity patient3 = new PatientEntity("D-3", "P-3", "Test", null, LocalDate.of(2000, 1, 1), "", "", "", "", "", "", "", "");
        FhirPatientDto fhirPatient3 = FhirPatientMapper.toFhir(patient3);
        assertThat(fhirPatient3.gender()).isEqualTo("unknown");
    }

    @Test
    void testPatientMapper_NullInput() {
        assertThat(FhirPatientMapper.toFhir(null)).isNull();
    }

    @Test
    void testEncounterMapper_FullDetails() {
        PatientEntity patient = new PatientEntity("D-1", "P-1", "John Doe", "MASCULIN", LocalDate.of(1990, 1, 1), "", "", "", "", "", "", "", "");
        UUID patientId = UUID.randomUUID();
        setField(patient, "id", patientId);

        VisitEntity visit = new VisitEntity(patient, "VISIT-001", "Consultation générale", "Orientation A");
        visit.setStatus("TERMINEE");
        Instant arrival = Instant.parse("2026-07-04T10:00:00Z");
        Instant closed = Instant.parse("2026-07-04T11:00:00Z");
        setField(visit, "createdAt", arrival);
        visit.setClosedAt(closed);

        FhirEncounterDto fhirEncounter = FhirEncounterMapper.toFhir(visit);

        assertThat(fhirEncounter).isNotNull();
        assertThat(fhirEncounter.resourceType()).isEqualTo("Encounter");
        assertThat(fhirEncounter.status()).isEqualTo("finished");
        assertThat(fhirEncounter.encounterClass().code()).isEqualTo("AMB");
        assertThat(fhirEncounter.encounterClass().display()).isEqualTo("ambulatory");
        assertThat(fhirEncounter.subject().reference()).isEqualTo("Patient/" + patientId);
        assertThat(fhirEncounter.period().start()).isEqualTo("2026-07-04T10:00:00Z");
        assertThat(fhirEncounter.period().end()).isEqualTo("2026-07-04T11:00:00Z");
        assertThat(fhirEncounter.identifier().get(0).value()).isEqualTo("VISIT-001");
    }

    @Test
    void testEncounterMapper_NullInput() {
        assertThat(FhirEncounterMapper.toFhir(null)).isNull();
    }

    @Test
    void testObservationMapper_FullDetails() {
        PatientEntity patient = new PatientEntity("D-1", "P-1", "John Doe", "MASCULIN", LocalDate.of(1990, 1, 1), "", "", "", "", "", "", "", "");
        UUID patientId = UUID.randomUUID();
        setField(patient, "id", patientId);

        VisitEntity visit = new VisitEntity(patient, "VISIT-001", "Consultation générale", "Orientation A");
        UUID visitId = UUID.randomUUID();
        setField(visit, "id", visitId);

        VitalsEntity vitals = new VitalsEntity(
                visit,
                BigDecimal.valueOf(37.2), // temp
                BigDecimal.valueOf(75.5), // weight
                175,                     // height
                72,                      // pulse
                120,                     // systolic
                80,                      // diastolic
                98,                      // spo2
                BigDecimal.valueOf(0.95), // glycemia
                16,                      // resp rate
                BigDecimal.valueOf(24.65) // bmi
        );
        Instant now = Instant.parse("2026-07-04T10:00:00Z");
        setField(vitals, "createdAt", now);

        List<FhirObservationDto> observations = FhirObservationMapper.toFhir(vitals);

        // Expected 9 observations: temp, weight, height, pulse, spo2, glycemia, resp-rate, bmi, blood pressure
        assertThat(observations).hasSize(9);

        // Let's verify specific observations
        FhirObservationDto tempObs = observations.stream()
                .filter(o -> o.code().coding().get(0).code().equals("8310-5"))
                .findFirst().orElseThrow();
        assertThat(tempObs.status()).isEqualTo("final");
        assertThat(tempObs.subject().reference()).isEqualTo("Patient/" + patientId);
        assertThat(tempObs.encounter().reference()).isEqualTo("Encounter/" + visitId);
        assertThat(tempObs.effectiveDateTime()).isEqualTo("2026-07-04T10:00:00Z");
        assertThat(tempObs.valueQuantity().value()).isEqualTo(BigDecimal.valueOf(37.2));
        assertThat(tempObs.valueQuantity().unit()).isEqualTo("°C");
        assertThat(tempObs.valueQuantity().code()).isEqualTo("Cel");

        // Verify blood pressure panel
        FhirObservationDto bpObs = observations.stream()
                .filter(o -> o.code().coding().get(0).code().equals("85354-9"))
                .findFirst().orElseThrow();
        assertThat(bpObs.valueQuantity()).isNull();
        assertThat(bpObs.component()).hasSize(2);
        
        FhirObservationDto.ObservationComponent systolicComp = bpObs.component().stream()
                .filter(c -> c.code().coding().get(0).code().equals("8480-6"))
                .findFirst().orElseThrow();
        assertThat(systolicComp.valueQuantity().value()).isEqualTo(BigDecimal.valueOf(120));
        assertThat(systolicComp.valueQuantity().unit()).isEqualTo("mmHg");
        assertThat(systolicComp.valueQuantity().code()).isEqualTo("mm[Hg]");

        FhirObservationDto.ObservationComponent diastolicComp = bpObs.component().stream()
                .filter(c -> c.code().coding().get(0).code().equals("8462-4"))
                .findFirst().orElseThrow();
        assertThat(diastolicComp.valueQuantity().value()).isEqualTo(BigDecimal.valueOf(80));
        assertThat(diastolicComp.valueQuantity().unit()).isEqualTo("mmHg");
        assertThat(diastolicComp.valueQuantity().code()).isEqualTo("mm[Hg]");
    }

    @Test
    void testObservationMapper_EmptyInput() {
        List<FhirObservationDto> observations = FhirObservationMapper.toFhir((com.joprelys.backend.visit.infrastructure.persistence.VitalsEntity) null);
        assertThat(observations).isEmpty();
    }
}
