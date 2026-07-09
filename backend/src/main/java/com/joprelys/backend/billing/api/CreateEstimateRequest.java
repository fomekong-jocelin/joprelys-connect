package com.joprelys.backend.billing.api;

import jakarta.validation.constraints.NotNull;
import java.util.List;
import java.util.UUID;

public record CreateEstimateRequest(
    @NotNull(message = "Le patient est obligatoire")
    UUID patientId,
    UUID visitId,
    List<EstimateItemRequest> items
) {}
