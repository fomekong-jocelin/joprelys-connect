package com.joprelys.backend.cash.api;

import jakarta.validation.constraints.NotNull;
import java.util.UUID;

public record OpenSessionRequest(
    UUID cashRegisterId,
    @NotNull(message = "Le solde d'ouverture est obligatoire")
    Double openingBalance
) {}
