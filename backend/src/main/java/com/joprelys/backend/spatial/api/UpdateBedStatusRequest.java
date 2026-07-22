package com.joprelys.backend.spatial.api;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record UpdateBedStatusRequest(
        @NotBlank(message = "Le statut du lit est obligatoire.")
        String status,
        @Size(max = 500, message = "La note ne peut pas dépasser 500 caractères.")
        String note
) {
}
