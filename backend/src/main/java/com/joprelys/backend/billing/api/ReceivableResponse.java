package com.joprelys.backend.billing.api;

import com.joprelys.backend.billing.infrastructure.persistence.ReceivableEntity;
import java.time.Instant;
import java.util.UUID;

public record ReceivableResponse(
    UUID id,
    UUID invoiceId,
    String debtorType,
    UUID debtorId,
    Double totalAmount,
    Double paidAmount,
    Double remainingAmount,
    String status,
    Instant dueDate,
    Instant createdAt,
    String agingSlice
) {
    public static ReceivableResponse fromEntity(ReceivableEntity entity) {
        double remaining = Math.max(0.0, entity.getTotalAmount() - entity.getPaidAmount());
        
        Instant created = entity.getCreatedAt() != null ? entity.getCreatedAt() : Instant.now();
        long days = java.time.temporal.ChronoUnit.DAYS.between(created, Instant.now());
        String slice;
        if (days <= 30) {
            slice = "0_30";
        } else if (days <= 60) {
            slice = "31_60";
        } else if (days <= 90) {
            slice = "61_90";
        } else {
            slice = "90_PLUS";
        }

        return new ReceivableResponse(
            entity.getId(), entity.getInvoiceId(),
            entity.getDebtorType(), entity.getDebtorId(),
            entity.getTotalAmount(), entity.getPaidAmount(),
            remaining, entity.getStatus(),
            entity.getDueDate(), entity.getCreatedAt(),
            slice
        );
    }
}
