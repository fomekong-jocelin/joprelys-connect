package com.joprelys.backend.fhir;

import com.joprelys.backend.visit.infrastructure.persistence.VitalsEntity;
import com.joprelys.backend.fhir.model.*;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

public class FhirObservationMapper {

    private static final List<CodeableConcept> VITAL_SIGNS_CATEGORY = List.of(
            new CodeableConcept(
                    new Coding("http://terminology.hl7.org/CodeSystem/observation-category", "vital-signs", "Vital Signs"),
                    "Vital Signs"
            )
    );

    private static final String LOINC_SYSTEM = "http://loinc.org";
    private static final String UCUM_SYSTEM = "http://unitsofmeasure.org";

    public static List<FhirObservationDto> toFhir(VitalsEntity vitals) {
        List<FhirObservationDto> observations = new ArrayList<>();
        if (vitals == null) {
            return observations;
        }

        String patientId = null;
        String encounterId = null;
        if (vitals.getVisit() != null) {
            encounterId = vitals.getVisit().getId() != null ? vitals.getVisit().getId().toString() : null;
            if (vitals.getVisit().getPatient() != null && vitals.getVisit().getPatient().getId() != null) {
                patientId = vitals.getVisit().getPatient().getId().toString();
            }
        }

        Reference subjectRef = patientId != null ? new Reference("Patient/" + patientId) : null;
        Reference encounterRef = encounterId != null ? new Reference("Encounter/" + encounterId) : null;
        String effectiveDateTime = vitals.getCreatedAt() != null ? vitals.getCreatedAt().toString() : null;

        // 1. Temperature
        if (vitals.getTemperature() != null) {
            observations.add(createSimpleObservation(
                    vitals.getId().toString() + "-temp",
                    subjectRef,
                    encounterRef,
                    effectiveDateTime,
                    "8310-5",
                    "Body temperature",
                    vitals.getTemperature(),
                    "°C",
                    "Cel"
            ));
        }

        // 2. Weight
        if (vitals.getWeight() != null) {
            observations.add(createSimpleObservation(
                    vitals.getId().toString() + "-weight",
                    subjectRef,
                    encounterRef,
                    effectiveDateTime,
                    "29463-7",
                    "Body weight",
                    vitals.getWeight(),
                    "kg",
                    "kg"
            ));
        }

        // 3. Height
        if (vitals.getHeight() != null) {
            observations.add(createSimpleObservation(
                    vitals.getId().toString() + "-height",
                    subjectRef,
                    encounterRef,
                    effectiveDateTime,
                    "8302-2",
                    "Body height",
                    BigDecimal.valueOf(vitals.getHeight()),
                    "cm",
                    "cm"
            ));
        }

        // 4. Pulse
        if (vitals.getPulse() != null) {
            observations.add(createSimpleObservation(
                    vitals.getId().toString() + "-pulse",
                    subjectRef,
                    encounterRef,
                    effectiveDateTime,
                    "8867-4",
                    "Heart rate",
                    BigDecimal.valueOf(vitals.getPulse()),
                    "/min",
                    "/min"
            ));
        }

        // 5. SpO2
        if (vitals.getSpo2() != null) {
            observations.add(createSimpleObservation(
                    vitals.getId().toString() + "-spo2",
                    subjectRef,
                    encounterRef,
                    effectiveDateTime,
                    "2708-6",
                    "Oxygen saturation in Arterial blood by Pulse oximetry",
                    BigDecimal.valueOf(vitals.getSpo2()),
                    "%",
                    "%"
            ));
        }

        // 6. Glycemia
        if (vitals.getGlycemia() != null) {
            observations.add(createSimpleObservation(
                    vitals.getId().toString() + "-glycemia",
                    subjectRef,
                    encounterRef,
                    effectiveDateTime,
                    "15074-8",
                    "Glucose [Mass/volume] in Blood",
                    vitals.getGlycemia(),
                    "g/L",
                    "g/L"
            ));
        }

        // 7. Respiratory Rate
        if (vitals.getRespiratoryRate() != null) {
            observations.add(createSimpleObservation(
                    vitals.getId().toString() + "-respiratory-rate",
                    subjectRef,
                    encounterRef,
                    effectiveDateTime,
                    "9279-1",
                    "Respiratory rate",
                    BigDecimal.valueOf(vitals.getRespiratoryRate()),
                    "/min",
                    "/min"
            ));
        }

        // 8. BMI
        if (vitals.getBmi() != null) {
            observations.add(createSimpleObservation(
                    vitals.getId().toString() + "-bmi",
                    subjectRef,
                    encounterRef,
                    effectiveDateTime,
                    "39156-5",
                    "Body mass index",
                    vitals.getBmi(),
                    "kg/m2",
                    "kg/m2"
            ));
        }

        // 9. Blood Pressure (composite observation)
        if (vitals.getSystolic() != null || vitals.getDiastolic() != null) {
            List<FhirObservationDto.ObservationComponent> components = new ArrayList<>();

            if (vitals.getSystolic() != null) {
                components.add(new FhirObservationDto.ObservationComponent(
                        new CodeableConcept(new Coding(LOINC_SYSTEM, "8480-6", "Systolic blood pressure"), "Systolic blood pressure"),
                        new Quantity(BigDecimal.valueOf(vitals.getSystolic()), "mmHg", UCUM_SYSTEM, "mm[Hg]")
                ));
            }

            if (vitals.getDiastolic() != null) {
                components.add(new FhirObservationDto.ObservationComponent(
                        new CodeableConcept(new Coding(LOINC_SYSTEM, "8462-4", "Diastolic blood pressure"), "Diastolic blood pressure"),
                        new Quantity(BigDecimal.valueOf(vitals.getDiastolic()), "mmHg", UCUM_SYSTEM, "mm[Hg]")
                ));
            }

            CodeableConcept bpCode = new CodeableConcept(
                    new Coding(LOINC_SYSTEM, "85354-9", "Blood pressure panel with all children"),
                    "Blood pressure panel with all children"
            );

            observations.add(new FhirObservationDto(
                    vitals.getId().toString() + "-bp",
                    "final",
                    VITAL_SIGNS_CATEGORY,
                    bpCode,
                    subjectRef,
                    encounterRef,
                    effectiveDateTime,
                    null,
                    components
            ));
        }

        return observations;
    }

    private static FhirObservationDto createSimpleObservation(
            String id,
            Reference subject,
            Reference encounter,
            String effectiveDateTime,
            String loincCode,
            String loincDisplay,
            BigDecimal value,
            String unit,
            String ucumCode
    ) {
        CodeableConcept code = new CodeableConcept(
                new Coding(LOINC_SYSTEM, loincCode, loincDisplay),
                loincDisplay
        );

        Quantity valueQuantity = new Quantity(value, unit, UCUM_SYSTEM, ucumCode);

        return new FhirObservationDto(
                id,
                "final",
                VITAL_SIGNS_CATEGORY,
                code,
                subject,
                encounter,
                effectiveDateTime,
                valueQuantity,
                null
        );
    }

    public static FhirObservationDto toFhir(com.joprelys.backend.lab.infrastructure.persistence.LabResultEntity result) {
        String patientId = result.getPatient() != null && result.getPatient().getId() != null
                ? result.getPatient().getId().toString() : null;
        String encounterId = result.getLabOrder() != null && result.getLabOrder().getId() != null
                ? result.getLabOrder().getId().toString() : null;

        Reference subjectRef = patientId != null ? new Reference("Patient/" + patientId) : null;
        Reference encounterRef = encounterId != null ? new Reference("Encounter/" + encounterId) : null;
        String effectiveDateTime = result.getValidatedAt() != null
                ? result.getValidatedAt().toString()
                : (result.getCreatedAt() != null ? result.getCreatedAt().toString() : null);

        CodeableConcept code = new CodeableConcept(
                new Coding("http://loinc.org", "laboratory", result.getAnalyteName()),
                result.getAnalyteName()
        );

        BigDecimal numericVal = null;
        try {
            numericVal = new BigDecimal(result.getValue());
        } catch (Exception ignored) {}

        Quantity valueQuantity = numericVal != null
                ? new Quantity(numericVal, result.getUnit(), "http://unitsofmeasure.org", result.getUnit())
                : null;

        List<CodeableConcept> category = List.of(
                new CodeableConcept(
                        new Coding("http://terminology.hl7.org/CodeSystem/observation-category", "laboratory", "Laboratory"),
                        "Laboratory"
                )
        );

        return new FhirObservationDto(
                result.getId().toString(),
                result.getStatus() != null ? result.getStatus().name().toLowerCase() : "final",
                category,
                code,
                subjectRef,
                encounterRef,
                effectiveDateTime,
                valueQuantity,
                null
        );
    }
}
