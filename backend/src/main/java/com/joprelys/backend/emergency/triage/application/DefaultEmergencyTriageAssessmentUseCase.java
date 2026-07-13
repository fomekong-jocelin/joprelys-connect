package com.joprelys.backend.emergency.triage.application;

import com.joprelys.backend.emergency.infrastructure.persistence.EmergencyEntity;
import com.joprelys.backend.emergency.infrastructure.persistence.EmergencyRepository;
import com.joprelys.backend.emergency.triage.infrastructure.persistence.EmergencyTriageAssessmentEntity;
import com.joprelys.backend.emergency.triage.infrastructure.persistence.EmergencyTriageAssessmentRepository;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class DefaultEmergencyTriageAssessmentUseCase implements EmergencyTriageAssessmentUseCase {

    private final EmergencyRepository emergencyRepository;
    private final EmergencyTriageAssessmentRepository assessmentRepository;

    public DefaultEmergencyTriageAssessmentUseCase(
            EmergencyRepository emergencyRepository,
            EmergencyTriageAssessmentRepository assessmentRepository) {
        this.emergencyRepository = emergencyRepository;
        this.assessmentRepository = assessmentRepository;
    }

    @Override
    @Transactional(propagation = Propagation.MANDATORY)
    public void recordInitial(
            EmergencyEntity emergency,
            EmergencyTriageAssessmentCommand command,
            UUID actorId) {
        if (assessmentRepository.existsByEmergency_Id(emergency.getId())) {
            return;
        }
        validateAssessedAt(command.assessedAt());
        assessmentRepository.save(EmergencyTriageAssessmentEntity.initial(emergency, command, actorId));
    }

    @Override
    @Transactional
    public EmergencyTriageAssessmentResult addReassessment(
            UUID emergencyId,
            EmergencyTriageAssessmentCommand command,
            UUID actorId) {
        EmergencyEntity emergency = emergencyRepository.findByIdForTriageUpdate(emergencyId)
                .orElseThrow(DefaultEmergencyTriageAssessmentUseCase::emergencyNotFound);
        if (emergency.getStabilizedAt() != null) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "EMERGENCY_ALREADY_STABILIZED");
        }

        validateAssessedAt(command.assessedAt());
        int nextSequence = assessmentRepository.findMaxSequenceNumber(emergencyId) + 1;
        EmergencyTriageAssessmentEntity saved = assessmentRepository.save(
                EmergencyTriageAssessmentEntity.reassessment(
                        emergency,
                        nextSequence,
                        command,
                        actorId));
        return toResult(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public List<EmergencyTriageAssessmentResult> getHistory(UUID emergencyId) {
        if (!emergencyRepository.existsById(emergencyId)) {
            throw emergencyNotFound();
        }
        return assessmentRepository.findByEmergency_IdOrderBySequenceNumberAsc(emergencyId).stream()
                .map(DefaultEmergencyTriageAssessmentUseCase::toResult)
                .toList();
    }

    private static void validateAssessedAt(Instant assessedAt) {
        if (assessedAt != null && assessedAt.isAfter(Instant.now())) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "EMERGENCY_TRIAGE_ASSESSED_AT_FUTURE");
        }
    }

    private static ResponseStatusException emergencyNotFound() {
        return new ResponseStatusException(HttpStatus.NOT_FOUND, "EMERGENCY_NOT_FOUND");
    }

    private static EmergencyTriageAssessmentResult toResult(EmergencyTriageAssessmentEntity entity) {
        return new EmergencyTriageAssessmentResult(
                entity.getId(),
                entity.getEmergency().getId(),
                entity.getAssessmentType(),
                entity.getSequenceNumber(),
                entity.getTriageLevel(),
                entity.getHemodynamicStatus(),
                entity.getAirwayStatus(),
                entity.getBreathingStatus(),
                entity.getCirculationStatus(),
                entity.getDisabilityStatus(),
                entity.getExposureStatus(),
                entity.getBpSystolic(),
                entity.getBpDiastolic(),
                entity.getHeartRate(),
                entity.getRespiratoryRate(),
                entity.getOxygenSaturation(),
                entity.getTemperature(),
                entity.getGcsScore(),
                entity.getPainScore(),
                entity.getRecommendedOrientation(),
                entity.getClinicalNotes(),
                entity.getAssessedAt(),
                entity.getAssessedByUserId(),
                entity.getCreatedAt());
    }
}