package com.joprelys.backend.fhir.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.util.List;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record CodeableConcept(
        List<Coding> coding,
        String text
) {
    public CodeableConcept(Coding coding, String text) {
        this(List.of(coding), text);
    }
}
