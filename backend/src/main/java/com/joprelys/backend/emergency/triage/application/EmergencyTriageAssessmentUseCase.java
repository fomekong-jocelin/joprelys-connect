package com.joprelys.backend.emergency.triage.application;

import com.joprelys.backend.emergency.infrastructure.persistence.EmergencyEntity;
import java.util.List;
import java.util.UUID;

public interface EmergencyTriageAssessmentUseCase {

    void recordInitial(
            EmergencyEntity emergency,
            EmergencyTriageAssessmentCommand command,
            UUID actorId);

    EmergencyTriageAssessmentResult addReassessment(
            UUID emergencyId,
            EmergencyTriageAssessmentCommand command,
            UUID actorId);

    List<EmergencyTriageAssessmentResult> getHistory(UUID emergencyId);
}