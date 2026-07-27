package com.joprelys.backend.ai.realtime.api;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record RealtimeIntakeRequest(
        @NotBlank @Size(max = 200) @Pattern(regexp = "[A-Za-z0-9._:-]+") String eventId,
        @Size(max = 200) @Pattern(regexp = "[A-Za-z0-9._:-]+") String itemId,
        @NotBlank @Size(max = 12000) String transcript,
        @DecimalMin("0.0") @DecimalMax("1.0") Double confidence) {
}
