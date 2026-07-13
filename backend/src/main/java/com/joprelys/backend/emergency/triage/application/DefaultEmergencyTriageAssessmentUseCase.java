package com.joprelys.backend.emergency.triage.application;

import com.joprelys.backend.audit.application.AuditService;
import com.joprelys.backend.auth.security.TenantContext;
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

    private static final String RESOURCE_TYPE = "EMERGENCY_TRIAGE";

    private final EmergencyRepository emergencyRepository;
    private final EmergencyTriageAssessmentRepository assessmentRepository;
    private final AuditService auditService;

    public DefaultEmergencyTriageAssessmentUseCase(
            EmergencyRepository emergencyRepository,
            EmergencyTriageAssessmentRepository assessmentRepository,
            AuditService auditService) {
        this.emergencyRepository = emergencyRepository;
        this.assessmentRepository = assessmentRepository;
        this.auditService = auditService;
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
        EmergencyTriageAssessmentEntity saved = assessmentRepository.save(
                EmergencyTriageAssessmentEntity.initial(emergency, command, actorId));
        audit(actorId, emergency, saved.getId(), "CREATE_INITIAL_TRIAGE", "Recorded initial triage assessment");
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

        validateReassessment(command);
        validateAssessedAt(command.assessedAt());
        int nextSequence = assessmentRepository.findMaxSequenceNumber(emergencyId) + 1;
        EmergencyTriageAssessmentEntity saved = assessmentRepository.save(
                EmergencyTriageAssessmentEntity.reassessment(
                        emergency,
                        nextSequence,
                        command,
                        actorId));
        audit(actorId, emergency, saved.getId(), "CREATE_TRIAGE_REASSESSMENT",
                "Recorded triage reassessment sequence " + nextSequence);
        return toResult(saved);
    }

    @Override
    @Transactional
    public List<EmergencyTriageAssessmentResult> getHistory(UUID emergencyId, UUID actorId) {
        EmergencyEntity emergency = requireEmergency(emergencyId);
        List<EmergencyTriageAssessmentResult> history = assessmentRepository
                .findByEmergency_IdOrderBySequenceNumberAsc(emergencyId).stream()
                .map(DefaultEmergencyTriageAssessmentUseCase::toResult)
                .toList();
        audit(actorId, emergency, emergencyId, "READ_TRIAGE_HISTORY", "Viewed triage assessment history");
        return history;
    }

    @Override
    @Transactional
    public EmergencyTriageAssessmentResult getAssessment(
            UUID emergencyId,
            UUID assessmentId,
            UUID actorId) {
        EmergencyEntity emergency = requireEmergency(emergencyId);
        EmergencyTriageAssessmentResult result = assessmentRepository
                .findByIdAndEmergency_Id(assessmentId, emergencyId)
                .map(DefaultEmergencyTriageAssessmentUseCase::toResult)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "EMERGENCY_TRIAGE_ASSESSMENT_NOT_FOUND"));
        audit(actorId, emergency, assessmentId, "READ_TRIAGE_ASSESSMENT", "Viewed one triage assessment");
        return result;
    }

    private EmergencyEntity requireEmergency(UUID emergencyId) {
        return emergencyRepository.findById(emergencyId)
                .orElseThrow(DefaultEmergencyTriageAssessmentUseCase::emergencyNotFound);
    }

    private void audit(
            UUID actorId,
            EmergencyEntity emergency,
            UUID resourceId,
            String action,
            String reason) {
        auditService.logSuccess(
                actorId,
                requireOrganizationId(),
                emergency.getPatient().getId(),
                RESOURCE_TYPE,
                resourceId,
                action,
                reason);
    }

    private UUID requireOrganizationId() {
        UUID organizationId = TenantContext.getTenantId();
        if (organizationId == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "AUTHENTICATION_REQUIRED");
        }
        return organizationId;
    }

    private static void validateReassessment(EmergencyTriageAssessmentCommand command) {
        if (isNotAssessed(command.airwayStatus())
                || isNotAssessed(command.breathingStatus())
                || isNotAssessed(command.circulationStatus())
                || isNotAssessed(command.disabilityStatus())
                || isNotAssessed(command.exposureStatus())) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "EMERGENCY_TRIAGE_ABCDE_REQUIRED");
        }
    }

    private static boolean isNotAssessed(Enum<?> value) {
        return value == null || "NOT_ASSESSED".equals(value.name());
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