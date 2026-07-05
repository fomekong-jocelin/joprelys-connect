package com.joprelys.backend.fhir.model;

import com.fasterxml.jackson.annotation.JsonInclude;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record Reference(
        String reference,
        String display
) {
    public Reference(String reference) {
        this(reference, null);
    }
}
