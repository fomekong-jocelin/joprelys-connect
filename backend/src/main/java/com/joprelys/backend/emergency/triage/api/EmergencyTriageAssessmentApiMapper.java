package com.joprelys.backend.emergency.triage.api;

import com.joprelys.backend.emergency.triage.application.EmergencyTriageAssessmentCommand;
import com.joprelys.backend.emergency.triage.application.EmergencyTriageAssessmentResult;

public final class EmergencyTriageAssessmentApiMapper {

    private EmergencyTriageAssessmentApiMapper() {
    }

    public static EmergencyTriageAssessmentCommand fromReassessment(
            CreateEmergencyTriageAssessmentRequest request) {
        EmergencyAbcdeAssessmentRequest abcde = request.abcdeAssessment();
        return new EmergencyTriageAssessmentCommand(
                request.triageLevel(),
                request.hemodynamicStatus(),
                abcde.airwayStatus(),
                abcde.breathingStatus(),
                abcde.circulationStatus(),
                abcde.disabilityStatus(),
                abcde.exposureStatus(),
                request.bpSystolic(),
                request.bpDiastolic(),
                request.heartRate(),
                abcde.respiratoryRate(),
                abcde.oxygenSaturation(),
                request.temperature(),
                abcde.gcsScore(),
                abcde.painScore(),
                abcde.recommendedOrientation(),
                abcde.clinicalNotes(),
                abcde.assessedAt());
    }

    public static EmergencyTriageAssessmentResponse toResponse(EmergencyTriageAssessmentResult result) {
        return new EmergencyTriageAssessmentResponse(
                result.id(),
                result.emergencyId(),
                result.assessmentType(),
                result.sequenceNumber(),
                result.triageLevel(),
                result.hemodynamicStatus(),
                result.airwayStatus(),
                result.breathingStatus(),
                result.circulationStatus(),
                result.disabilityStatus(),
                result.exposureStatus(),
                result.bpSystolic(),
                result.bpDiastolic(),
                result.heartRate(),
                result.respiratoryRate(),
                result.oxygenSaturation(),
                result.temperature(),
                result.gcsScore(),
                result.painScore(),
                result.recommendedOrientation(),
                result.clinicalNotes(),
                result.assessedAt(),
                result.assessedByUserId(),
                result.createdAt());
    }
}