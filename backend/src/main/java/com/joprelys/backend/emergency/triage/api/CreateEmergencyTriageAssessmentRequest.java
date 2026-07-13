package com.joprelys.backend.emergency.triage.api;

import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import java.math.BigDecimal;

public record CreateEmergencyTriageAssessmentRequest(
        @NotBlank(message = "EMERGENCY_TRIAGE_LEVEL_REQUIRED")
        @Pattern(
                regexp = "RED|ORANGE|YELLOW|GREEN",
                message = "EMERGENCY_TRIAGE_LEVEL_INVALID")
        String triageLevel,

        @NotBlank(message = "EMERGENCY_HEMODYNAMIC_STATUS_REQUIRED")
        @Pattern(
                regexp = "SHOCK|UNSTABLE|STABLE",
                message = "EMERGENCY_HEMODYNAMIC_STATUS_INVALID")
        String hemodynamicStatus,

        @Min(value = 30, message = "EMERGENCY_TRIAGE_BP_SYSTOLIC_INVALID")
        @Max(value = 300, message = "EMERGENCY_TRIAGE_BP_SYSTOLIC_INVALID")
        Integer bpSystolic,

        @Min(value = 20, message = "EMERGENCY_TRIAGE_BP_DIASTOLIC_INVALID")
        @Max(value = 200, message = "EMERGENCY_TRIAGE_BP_DIASTOLIC_INVALID")
        Integer bpDiastolic,

        @Min(value = 20, message = "EMERGENCY_TRIAGE_HEART_RATE_INVALID")
        @Max(value = 300, message = "EMERGENCY_TRIAGE_HEART_RATE_INVALID")
        Integer heartRate,

        @DecimalMin(value = "25.0", message = "EMERGENCY_TRIAGE_TEMPERATURE_INVALID")
        @DecimalMax(value = "45.0", message = "EMERGENCY_TRIAGE_TEMPERATURE_INVALID")
        BigDecimal temperature,

        @NotNull(message = "EMERGENCY_TRIAGE_ASSESSMENT_REQUIRED")
        @Valid
        EmergencyAbcdeAssessmentRequest abcdeAssessment) {
}