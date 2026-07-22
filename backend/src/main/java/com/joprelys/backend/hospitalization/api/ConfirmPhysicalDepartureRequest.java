package com.joprelys.backend.hospitalization.api;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record ConfirmPhysicalDepartureRequest(
        @NotNull(message = "La confirmation explicite du départ physique est obligatoire.")
        @AssertTrue(message = "Le départ physique doit être explicitement confirmé.")
        Boolean confirmed,
        @Size(max = 500, message = "La note de départ physique ne peut pas dépasser 500 caractères.")
        String note
) {
}
