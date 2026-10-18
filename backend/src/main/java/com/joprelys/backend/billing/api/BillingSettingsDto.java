package com.joprelys.backend.billing.api;

import com.joprelys.backend.clinic.infrastructure.persistence.OrganizationEntity;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import java.math.BigDecimal;

public record BillingSettingsDto(
        boolean labPaymentBeforeSampling,
        @NotNull(message = "Le seuil d'approbation des avoirs est obligatoire.")
        @PositiveOrZero(message = "Le seuil d'approbation des avoirs doit être positif ou nul.")
        BigDecimal creditNoteApprovalThreshold
) {
    public static BillingSettingsDto from(OrganizationEntity organization) {
        return new BillingSettingsDto(organization.isLabPaymentBeforeSampling(), organization.getCreditNoteApprovalThreshold());
    }
}
