package com.joprelys.backend.emergency.triage.api;

import com.joprelys.backend.emergency.api.CreateEmergencyRequest;
import com.joprelys.backend.emergency.triage.application.EmergencyTriageAssessmentCommand;
import com.joprelys.backend.emergency.triage.application.EmergencyTriageAssessmentResult;
import com.joprelys.backend.emergency.triage.domain.EmergencyTriageVocabulary.AirwayStatus;
import com.joprelys.backend.emergency.triage.domain.EmergencyTriageVocabulary.BreathingStatus;
import com.joprelys.backend.emergency.triage.domain.EmergencyTriageVocabulary.CirculationStatus;
import com.joprelys.backend.emergency.triage.domain.EmergencyTriageVocabulary.DisabilityStatus;
import com.joprelys.backend.emergency.triage.domain.EmergencyTriageVocabulary.ExposureStatus;

public final class EmergencyTriageAssessmentApiMapper {

    private EmergencyTriageAssessmentApiMapper() {
    }

    public static EmergencyTriageAssessmentCommand fromInitial(CreateEmergencyRequest request) {
        EmergencyAbcdeAssessmentRequest abcde = request.abcdeAssessment();
        return new EmergencyTriageAssessmentCommand(
                request.triageLevel(),
                request.hemodynamicStatus(),
                abcde == null ? AirwayStatus.NOT_ASSESSED : abcde.airwayStatus(),
                abcde == null ? BreathingStatus.NOT_ASSESSED : abcde.breathingStatus(),
                abcde == null ? CirculationStatus.NOT_ASSESSED : abcde.circulationStatus(),
                abcde == null ? DisabilityStatus.NOT_ASSESSED : abcde.disabilityStatus(),
                abcde == null ? ExposureStatus.NOT_ASSESSED : abcde.exposureStatus(),
                request.initialBpSystolic(),
                request.initialBpDiastolic(),
                request.initialHr(),
                abcde == null ? null : abcde.respiratoryRate(),
                abcde == null ? null : abcde.oxygenSaturation(),
                request.initialTemp(),
                abcde == null ? null : abcde.gcsScore(),
                abcde == null ? null : abcde.painScore(),
                abcde == null ? null : abcde.recommendedOrientation(),
                abcde == null ? null : abcde.clinicalNotes(),
                abcde == null ? null : abcde.assessedAt());
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