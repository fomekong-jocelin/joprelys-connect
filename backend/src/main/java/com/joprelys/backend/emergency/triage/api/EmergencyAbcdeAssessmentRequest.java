package com.joprelys.backend.emergency.triage.api;

import com.joprelys.backend.emergency.triage.domain.EmergencyTriageVocabulary.AirwayStatus;
import com.joprelys.backend.emergency.triage.domain.EmergencyTriageVocabulary.BreathingStatus;
import com.joprelys.backend.emergency.triage.domain.EmergencyTriageVocabulary.CirculationStatus;
import com.joprelys.backend.emergency.triage.domain.EmergencyTriageVocabulary.DisabilityStatus;
import com.joprelys.backend.emergency.triage.domain.EmergencyTriageVocabulary.ExposureStatus;
import com.joprelys.backend.emergency.triage.domain.EmergencyTriageVocabulary.RecommendedOrientation;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PastOrPresent;
import jakarta.validation.constraints.Size;
import java.time.Instant;

public record EmergencyAbcdeAssessmentRequest(
        @NotNull(message = "EMERGENCY_TRIAGE_AIRWAY_REQUIRED")
        AirwayStatus airwayStatus,

        @NotNull(message = "EMERGENCY_TRIAGE_BREATHING_REQUIRED")
        BreathingStatus breathingStatus,

        @NotNull(message = "EMERGENCY_TRIAGE_CIRCULATION_REQUIRED")
        CirculationStatus circulationStatus,

        @NotNull(message = "EMERGENCY_TRIAGE_DISABILITY_REQUIRED")
        DisabilityStatus disabilityStatus,

        @NotNull(message = "EMERGENCY_TRIAGE_EXPOSURE_REQUIRED")
        ExposureStatus exposureStatus,

        @Min(value = 0, message = "EMERGENCY_TRIAGE_RESPIRATORY_RATE_INVALID")
        @Max(value = 100, message = "EMERGENCY_TRIAGE_RESPIRATORY_RATE_INVALID")
        Integer respiratoryRate,

        @Min(value = 0, message = "EMERGENCY_TRIAGE_OXYGEN_SATURATION_INVALID")
        @Max(value = 100, message = "EMERGENCY_TRIAGE_OXYGEN_SATURATION_INVALID")
        Integer oxygenSaturation,

        @Min(value = 3, message = "EMERGENCY_TRIAGE_GCS_INVALID")
        @Max(value = 15, message = "EMERGENCY_TRIAGE_GCS_INVALID")
        Integer gcsScore,

        @Min(value = 0, message = "EMERGENCY_TRIAGE_PAIN_SCORE_INVALID")
        @Max(value = 10, message = "EMERGENCY_TRIAGE_PAIN_SCORE_INVALID")
        Integer painScore,

        RecommendedOrientation recommendedOrientation,

        @Size(max = 4000, message = "EMERGENCY_TRIAGE_NOTES_TOO_LONG")
        String clinicalNotes,

        @PastOrPresent(message = "EMERGENCY_TRIAGE_ASSESSED_AT_FUTURE")
        Instant assessedAt) {
}