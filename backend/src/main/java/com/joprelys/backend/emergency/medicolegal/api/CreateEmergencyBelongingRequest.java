package com.joprelys.backend.emergency.medicolegal.api;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateEmergencyBelongingRequest(
        @NotBlank(message = "EMERGENCY_BELONGING_CATEGORY_REQUIRED")
        @Size(max = 48, message = "EMERGENCY_BELONGING_CATEGORY_TOO_LONG")
        String category,

        @NotBlank(message = "EMERGENCY_BELONGING_DESCRIPTION_REQUIRED")
        @Size(max = 500, message = "EMERGENCY_BELONGING_DESCRIPTION_TOO_LONG")
        String description,

        @Min(value = 1, message = "EMERGENCY_BELONGING_QUANTITY_INVALID")
        int quantity,

        @Size(max = 255, message = "EMERGENCY_BELONGING_CONDITION_TOO_LONG")
        String itemCondition,

        @Size(max = 80, message = "EMERGENCY_BELONGING_SEAL_TOO_LONG")
        String sealNumber,

        @Size(max = 160, message = "EMERGENCY_BELONGING_DEPOSITOR_TOO_LONG")
        String depositedByName) {
}
