package com.joprelys.backend.spatial.api;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.UUID;

public record SaveRoomRequest(
        @NotNull(message = "Le service est obligatoire.") UUID wardId,
        @NotBlank(message = "Le numéro de chambre est obligatoire.")
        @Size(max = 20, message = "Le numéro de chambre ne peut pas dépasser 20 caractères.")
        String roomNumber,
        @NotNull(message = "La capacité est obligatoire.")
        @Min(value = 1, message = "La capacité doit être supérieure à zéro.")
        @Max(value = 100, message = "La capacité ne peut pas dépasser 100 lits.")
        Integer capacity,
        @NotBlank(message = "Le niveau de confort est obligatoire.")
        @Size(max = 50, message = "Le niveau de confort ne peut pas dépasser 50 caractères.")
        String comfortLevel
) {
}
