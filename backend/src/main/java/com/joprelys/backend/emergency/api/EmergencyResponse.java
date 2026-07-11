package com.joprelys.backend.emergency.api;

import com.joprelys.backend.emergency.infrastructure.persistence.EmergencyEntity;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * General clinical emergency response.
 *
 * <p>Medico-legal third-party identities, proofs and custody data are intentionally
 * excluded. They are exposed only through the protected medico-legal endpoint.</p>
 */
public record EmergencyResponse(
        UUID id,
        UUID organizationId,
        UUID patientId,
        String patientName,
        String globalPatientNumber,
        String localPatientNumber,
        String temporaryPatientNumber,
        String identityStatus,
        String identityConfidenceLevel,
        String apparentGender,
        String estimatedAgeRange,
        String physicalDescription,
        Instant foundAt,
        String foundLocation,
        UUID visitId,
        String arrivalMode,
        String triageLevel,
        String hemodynamicStatus,
        String chiefComplaint,
        Integer initialBpSystolic,
        Integer initialBpDiastolic,
        Integer initialHr,
        BigDecimal initialTemp,
        boolean thirdPartyRecorded,
        Instant stabilizedAt,
        String orientation,
        UUID createdByUserId,
        List<ResuscitationLogResponse> resuscitationLogs,
        Instant createdAt,
        Instant updatedAt) {

    public static EmergencyResponse fromEntity(EmergencyEntity entity) {
        var patient = entity.getPatient();
        var logs = entity.getResuscitationLogs() == null
                ? List.<ResuscitationLogResponse>of()
                : entity.getResuscitationLogs().stream()
                        .map(ResuscitationLogResponse::fromEntity)
                        .toList();

        return new EmergencyResponse(
                entity.getId(),
                entity.getOrganizationId(),
                patient.getId(),
                patient.getDisplayName(),
                patient.getGlobalPatientNumber(),
                patient.getLocalPatientNumber(),
                patient.getTemporaryPatientNumber(),
                patient.getIdentityStatus() == null ? null : patient.getIdentityStatus().name(),
                patient.getIdentityConfidenceLevel() == null
                        ? null
                        : patient.getIdentityConfidenceLevel().name(),
                patient.getApparentGender(),
                patient.getEstimatedAgeRange(),
                patient.getPhysicalDescription(),
                patient.getFoundAt(),
                patient.getFoundLocation(),
                entity.getVisitId(),
                entity.getArrivalMode(),
                entity.getTriageLevel(),
                entity.getHemodynamicStatus(),
                entity.getChiefComplaint(),
                entity.getInitialBpSystolic(),
                entity.getInitialBpDiastolic(),
                entity.getInitialHr(),
                entity.getInitialTemp(),
                entity.getThirdPartyName() != null,
                entity.getStabilizedAt(),
                entity.getOrientation(),
                entity.getCreatedByUserId(),
                logs,
                entity.getCreatedAt(),
                entity.getUpdatedAt());
    }
}
