package com.joprelys.backend.emergency.medicolegal.api;

import com.joprelys.backend.emergency.medicolegal.domain.EmergencyCapacityStatus;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.Instant;

public record RecordEmergencyCapacityRequest(
        @NotNull(message = "EMERGENCY_CAPACITY_STATUS_REQUIRED")
        EmergencyCapacityStatus status,

        @Size(max = 32, message = "EMERGENCY_CONSCIOUSNESS_LEVEL_TOO_LONG")
        String consciousnessLevel,

        @NotBlank(message = "EMERGENCY_CAPACITY_REASON_REQUIRED")
        @Size(max = 1000, message = "EMERGENCY_CAPACITY_REASON_TOO_LONG")
        String clinicalReason,

        Instant effectiveAt) {
}
