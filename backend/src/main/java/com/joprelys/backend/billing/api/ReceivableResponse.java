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
    Instant createdAt
) {
    public static ReceivableResponse fromEntity(ReceivableEntity entity) {
        double remaining = Math.max(0.0, entity.getTotalAmount() - entity.getPaidAmount());
        return new ReceivableResponse(
            entity.getId(), entity.getInvoiceId(),
            entity.getDebtorType(), entity.getDebtorId(),
            entity.getTotalAmount(), entity.getPaidAmount(),
            remaining, entity.getStatus(),
            entity.getDueDate(), entity.getCreatedAt()
        );
    }
}
