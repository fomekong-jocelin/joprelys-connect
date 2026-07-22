package com.joprelys.backend.hospitalization.api;

import com.joprelys.backend.hospitalization.infrastructure.persistence.HospitalizationEntity;
import java.time.Instant;
import java.util.UUID;

public record HospitalizationResponse(
        UUID id,
        UUID patientId,
        UUID organizationId,
        Long version,
        String serviceName,
        String roomNumber,
        String bedNumber,
        String admissionReason,
        String status,
        Instant admittedAt,
        Instant dischargedAt,
        String dischargeDiagnosis,
        String dischargeInstructions,
        Instant dischargeDecidedAt,
        UUID dischargeDecidedBy,
        Boolean dischargeAgainstMedicalAdvice,
        Instant physicalDepartureAt,
        UUID physicalDepartureBy,
        String physicalDepartureNote,
        String pdfFilePath,
        String hospitalizationNumber,
        UUID visitId,
        UUID emergencyId,
        UUID responsiblePractitionerId,
        UUID documentId
) {
    public static HospitalizationResponse fromEntity(HospitalizationEntity entity) {
        return new HospitalizationResponse(
                entity.getId(),
                entity.getPatientId(),
                entity.getOrganizationId(),
                entity.getVersion(),
                entity.getServiceName(),
                entity.getRoomNumber(),
                entity.getBedNumber(),
                entity.getAdmissionReason(),
                entity.getStatus(),
                entity.getAdmittedAt(),
                entity.getDischargedAt(),
                entity.getDischargeDiagnosis(),
                entity.getDischargeInstructions(),
                entity.getDischargeDecidedAt(),
                entity.getDischargeDecidedBy(),
                entity.getDischargeAgainstMedicalAdvice(),
                entity.getPhysicalDepartureAt(),
                entity.getPhysicalDepartureBy(),
                entity.getPhysicalDepartureNote(),
                entity.getPdfFilePath(),
                entity.getHospitalizationNumber(),
                entity.getVisitId(),
                entity.getEmergencyId(),
                entity.getResponsiblePractitionerId(),
                entity.getDocumentId()
        );
    }
}
