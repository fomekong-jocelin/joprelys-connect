package com.joprelys.backend.fhir.model;

import com.fasterxml.jackson.annotation.JsonInclude;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record Period(
        String start,
        String end
) {}
