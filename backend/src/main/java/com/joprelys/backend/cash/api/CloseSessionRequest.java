package com.joprelys.backend.cash.api;

import jakarta.validation.constraints.NotNull;

public record CloseSessionRequest(
    @NotNull(message = "Le solde déclaré est obligatoire")
    Double declaredBalance,
    String discrepancyReason
) {}
