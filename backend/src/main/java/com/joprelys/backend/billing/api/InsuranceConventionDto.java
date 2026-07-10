package com.joprelys.backend.billing.api;

import com.joprelys.backend.billing.infrastructure.persistence.InsuranceConventionEntity;
import java.math.BigDecimal;
import java.util.UUID;

public record InsuranceConventionDto(
        UUID id,
        String name,
        BigDecimal coveragePercentage
) {
    public static InsuranceConventionDto fromEntity(InsuranceConventionEntity entity) {
        return new InsuranceConventionDto(
                entity.getId(),
                entity.getName(),
                entity.getCoveragePercentage()
        );
    }
}
