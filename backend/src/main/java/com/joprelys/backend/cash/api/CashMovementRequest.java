package com.joprelys.backend.cash.api;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record CashMovementRequest(
    @NotBlank(message = "Le type de mouvement est obligatoire")
    String movementType, // IN, OUT, TRANSFER_TO_BANK
    @NotNull(message = "Le montant est obligatoire")
    Double amount,
    @NotBlank(message = "La description est obligatoire")
    String description,
    @NotBlank(message = "Le mode de paiement est obligatoire")
    String paymentMethod,
    String referenceNumber,
    Boolean doubleVisaApproved // pour passer outre la restriction > 100 000 FCFA si double visa
) {}
