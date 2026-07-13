package com.joprelys.backend.emergency.triage.application;

import com.joprelys.backend.emergency.triage.domain.EmergencyTriageVocabulary.AirwayStatus;
import com.joprelys.backend.emergency.triage.domain.EmergencyTriageVocabulary.BreathingStatus;
import com.joprelys.backend.emergency.triage.domain.EmergencyTriageVocabulary.CirculationStatus;
import com.joprelys.backend.emergency.triage.domain.EmergencyTriageVocabulary.DisabilityStatus;
import com.joprelys.backend.emergency.triage.domain.EmergencyTriageVocabulary.ExposureStatus;
import com.joprelys.backend.emergency.triage.domain.EmergencyTriageVocabulary.RecommendedOrientation;
import java.math.BigDecimal;
import java.time.Instant;

public record EmergencyTriageAssessmentCommand(
        String triageLevel,
        String hemodynamicStatus,
        AirwayStatus airwayStatus,
        BreathingStatus breathingStatus,
        CirculationStatus circulationStatus,
        DisabilityStatus disabilityStatus,
        ExposureStatus exposureStatus,
        Integer bpSystolic,
        Integer bpDiastolic,
        Integer heartRate,
        Integer respiratoryRate,
        Integer oxygenSaturation,
        BigDecimal temperature,
        Integer gcsScore,
        Integer painScore,
        RecommendedOrientation recommendedOrientation,
        String clinicalNotes,
        Instant assessedAt) {
}