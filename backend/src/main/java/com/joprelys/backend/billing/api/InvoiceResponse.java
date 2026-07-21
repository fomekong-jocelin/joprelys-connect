package com.joprelys.backend.billing.api;

import com.joprelys.backend.billing.domain.InvoiceRegularizationStatus;
import com.joprelys.backend.billing.infrastructure.persistence.InvoiceEntity;
import com.joprelys.backend.billing.infrastructure.persistence.InvoiceStatus;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record InvoiceResponse(
        UUID id,
        UUID patientId,
        UUID visitId,
        String invoiceNumber,
        InsuranceConventionDto insuranceConvention,
        BigDecimal totalAmount,
        BigDecimal patientShare,
        BigDecimal insuranceShare,
        InvoiceStatus status,
        InvoiceRegularizationStatus regularizationStatus,
        List<InvoiceItemResponse> items,
        Instant createdAt,
        Instant validatedAt,
        UUID validatedByUserId,
        BigDecimal discountAmount,
        String discountReason
) {
    public static InvoiceResponse fromEntity(InvoiceEntity entity) {
        return new InvoiceResponse(
                entity.getId(),
                entity.getPatientId(),
                entity.getVisitId(),
                entity.getInvoiceNumber(),
                entity.getInsuranceConvention() != null ? InsuranceConventionDto.fromEntity(entity.getInsuranceConvention()) : null,
                entity.getTotalAmount(),
                entity.getPatientShare(),
                entity.getInsuranceShare(),
                entity.getStatus(),
                entity.getRegularizationStatus(),
                entity.getItems().stream().map(InvoiceItemResponse::fromEntity).toList(),
                entity.getCreatedAt(),
                entity.getValidatedAt(),
                entity.getValidatedByUserId(),
                entity.getDiscountAmount(),
                entity.getDiscountReason()
        );
    }
}
