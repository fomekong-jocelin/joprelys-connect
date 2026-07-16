package com.joprelys.backend.spatial.api;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.UUID;

public record SaveBedRequest(
        @NotNull(message = "La chambre est obligatoire.") UUID roomId,
        @NotBlank(message = "Le numéro de lit est obligatoire.")
        @Size(max = 20, message = "Le numéro de lit ne peut pas dépasser 20 caractères.")
        String bedNumber
) {
}
