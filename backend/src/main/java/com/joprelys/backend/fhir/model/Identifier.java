package com.joprelys.backend.fhir.model;

import com.fasterxml.jackson.annotation.JsonInclude;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record Identifier(
        String system,
        String value
) {
    public Identifier(String value) {
        this(null, value);
    }
}
