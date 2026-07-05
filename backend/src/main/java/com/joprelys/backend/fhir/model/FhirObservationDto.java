package com.joprelys.backend.fhir.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.util.List;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record FhirObservationDto(
        String resourceType,
        String id,
        String status,
        List<CodeableConcept> category,
        CodeableConcept code,
        Reference subject,
        Reference encounter,
        String effectiveDateTime,
        Quantity valueQuantity,
        List<ObservationComponent> component
) {
    public FhirObservationDto(
            String id,
            String status,
            List<CodeableConcept> category,
            CodeableConcept code,
            Reference subject,
            Reference encounter,
            String effectiveDateTime,
            Quantity valueQuantity,
            List<ObservationComponent> component
    ) {
        this("Observation", id, status, category, code, subject, encounter, effectiveDateTime, valueQuantity, component);
    }

    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record ObservationComponent(
            CodeableConcept code,
            Quantity valueQuantity
    ) {}
}
