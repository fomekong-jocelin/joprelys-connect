package com.joprelys.backend.spatial.api;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record UpdateBedCapacityStatusRequest(
        @NotBlank(message = "L'état de capacité est obligatoire.")
        String status,
        @NotBlank(message = "Le motif du changement de capacité est obligatoire.")
        String reasonCode,
        @Size(max = 500, message = "La note ne peut pas dépasser 500 caractères.")
        String note
) {
}
