package com.joprelys.backend.hospitalization.api;

import com.joprelys.backend.hospitalization.infrastructure.persistence.SurgicalImplantEntity;
import java.util.UUID;

public record SurgicalImplantResponse(
    UUID id,
    UUID operatingReportId,
    UUID hospitalizationId,
    String implantName,
    String lotNumber,
    int quantity,
    double unitPrice,
    String manufacturer
) {
    public static SurgicalImplantResponse fromEntity(SurgicalImplantEntity entity) {
        return new SurgicalImplantResponse(
            entity.getId(),
            entity.getOperatingReport().getId(),
            entity.getHospitalizationId(),
            entity.getImplantName(),
            entity.getLotNumber(),
            entity.getQuantity(),
            entity.getUnitPrice(),
            entity.getManufacturer()
        );
    }
}
