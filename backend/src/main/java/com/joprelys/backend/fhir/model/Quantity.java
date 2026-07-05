package com.joprelys.backend.fhir.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.math.BigDecimal;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record Quantity(
        BigDecimal value,
        String unit,
        String system,
        String code
) {}
