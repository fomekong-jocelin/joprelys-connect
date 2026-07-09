package com.joprelys.backend.hospitalization.api;

import com.joprelys.backend.hospitalization.infrastructure.persistence.OperatingReportEntity;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record OperatingReportResponse(
    UUID id,
    UUID hospitalizationId,
    UUID surgeonId,
    UUID anesthetistId,
    Instant operationDate,
    String preOperativeDiagnosis,
    String postOperativeDiagnosis,
    String procedureName,
    String procedureDescription,
    String anesthesiaType,
    String anesthesiaDescription,
    double kSurgeonValue,
    double kAnesthesistValue,
    double kBlocValue,
    boolean validated,
    String validatedBy,
    Instant validatedAt,
    List<SurgicalImplantResponse> implants,
    Instant createdAt
) {
    public static OperatingReportResponse fromEntity(OperatingReportEntity entity) {
        List<SurgicalImplantResponse> implantList = entity.getImplants() != null
                ? entity.getImplants().stream().map(SurgicalImplantResponse::fromEntity).toList()
                : List.of();

        return new OperatingReportResponse(
            entity.getId(),
            entity.getHospitalizationId(),
            entity.getSurgeonId(),
            entity.getAnesthetistId(),
            entity.getOperationDate(),
            entity.getPreOperativeDiagnosis(),
            entity.getPostOperativeDiagnosis(),
            entity.getProcedureName(),
            entity.getProcedureDescription(),
            entity.getAnesthesiaType(),
            entity.getAnesthesiaDescription(),
            entity.getkSurgeonValue(),
            entity.getkAnesthesistValue(),
            entity.getkBlocValue(),
            entity.isValidated(),
            entity.getValidatedBy(),
            entity.getValidatedAt(),
            implantList,
            entity.getCreatedAt()
        );
    }
}
