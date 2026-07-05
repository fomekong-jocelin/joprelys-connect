package com.joprelys.backend.fhir.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record FhirEncounterDto(
        String resourceType,
        String id,
        List<Identifier> identifier,
        String status,
        @JsonProperty("class") Coding encounterClass,
        Reference subject,
        Period period
) {
    public FhirEncounterDto(
            String id,
            List<Identifier> identifier,
            String status,
            Coding encounterClass,
            Reference subject,
            Period period
    ) {
        this("Encounter", id, identifier, status, encounterClass, subject, period);
    }
}
