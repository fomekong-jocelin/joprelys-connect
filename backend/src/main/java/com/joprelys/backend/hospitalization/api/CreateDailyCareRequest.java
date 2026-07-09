package com.joprelys.backend.hospitalization.api;

import jakarta.validation.constraints.NotBlank;
import java.time.Instant;

public record CreateDailyCareRequest(
    @NotBlank(message = "Le type de soin est obligatoire")
    String careType,
    String description,
    boolean billable,
    Double price,
    Instant performedAt
) {}
