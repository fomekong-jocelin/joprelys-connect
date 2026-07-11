package com.joprelys.backend.emergency.api;

import com.joprelys.backend.emergency.infrastructure.persistence.EmergencyEntity;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

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
        String thirdPartyName,
        String thirdPartyPhone,
        String thirdPartyRelationship,
        String thirdPartyIdDocument,
        String thirdPartyCircumstances,
        boolean thirdPartyConsentToContact,
        Instant stabilizedAt,
        String orientation,
        UUID createdByUserId,
        List<ResuscitationLogResponse> resuscitationLogs,
        Instant createdAt,
        Instant updatedAt
) {
    public static EmergencyResponse fromEntity(EmergencyEntity entity) {
        var patient = entity.getPatient();
        var logs = entity.getResuscitationLogs() != null
                ? entity.getResuscitationLogs().stream().map(ResuscitationLogResponse::fromEntity).toList()
                : List.<ResuscitationLogResponse>of();

        return new EmergencyResponse(
                entity.getId(),
                entity.getOrganizationId(),
                patient.getId(),
                patient.getDisplayName(),
                patient.getGlobalPatientNumber(),
                patient.getLocalPatientNumber(),
                patient.getTemporaryPatientNumber(),
                patient.getIdentityStatus() != null ? patient.getIdentityStatus().name() : null,
                patient.getIdentityConfidenceLevel() != null ? patient.getIdentityConfidenceLevel().name() : null,
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
                entity.getThirdPartyName(),
                entity.getThirdPartyPhone(),
                entity.getThirdPartyRelationship(),
                entity.getThirdPartyIdDocument(),
                entity.getThirdPartyCircumstances(),
                entity.isThirdPartyConsentToContact(),
                entity.getStabilizedAt(),
                entity.getOrientation(),
                entity.getCreatedByUserId(),
                logs,
                entity.getCreatedAt(),
                entity.getUpdatedAt()
        );
    }
}
