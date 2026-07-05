package com.joprelys.backend.fhir.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.util.List;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record FhirPatientDto(
        String resourceType,
        String id,
        List<Identifier> identifier,
        List<HumanName> name,
        String gender,
        String birthDate,
        List<ContactPoint> telecom
) {
    public FhirPatientDto(
            String id,
            List<Identifier> identifier,
            List<HumanName> name,
            String gender,
            String birthDate,
            List<ContactPoint> telecom
    ) {
        this("Patient", id, identifier, name, gender, birthDate, telecom);
    }
}
