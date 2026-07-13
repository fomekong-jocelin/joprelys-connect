package com.joprelys.backend.emergency.triage.api;

import com.joprelys.backend.emergency.triage.domain.EmergencyTriageVocabulary.AirwayStatus;
import com.joprelys.backend.emergency.triage.domain.EmergencyTriageVocabulary.AssessmentType;
import com.joprelys.backend.emergency.triage.domain.EmergencyTriageVocabulary.BreathingStatus;
import com.joprelys.backend.emergency.triage.domain.EmergencyTriageVocabulary.CirculationStatus;
import com.joprelys.backend.emergency.triage.domain.EmergencyTriageVocabulary.DisabilityStatus;
import com.joprelys.backend.emergency.triage.domain.EmergencyTriageVocabulary.ExposureStatus;
import com.joprelys.backend.emergency.triage.domain.EmergencyTriageVocabulary.RecommendedOrientation;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record EmergencyTriageAssessmentResponse(
        UUID id,
        UUID emergencyId,
        AssessmentType assessmentType,
        int sequenceNumber,
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
        Instant assessedAt,
        UUID assessedByUserId,
        Instant createdAt) {
}