package com.joprelys.backend.billing.api;

import com.joprelys.backend.billing.infrastructure.persistence.InsuranceConventionEntity;
import java.util.UUID;

public record InsuranceConventionDto(
        UUID id,
        String name,
        Double coveragePercentage
) {
    public static InsuranceConventionDto fromEntity(InsuranceConventionEntity entity) {
        return new InsuranceConventionDto(entity.getId(), entity.getName(), entity.getCoveragePercentage());
    }
}
