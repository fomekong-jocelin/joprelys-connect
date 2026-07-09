package com.joprelys.backend.cash.api;

import com.joprelys.backend.cash.infrastructure.persistence.PaymentReceiptEntity;
import java.time.Instant;
import java.util.UUID;

public record PaymentReceiptResponse(
    UUID id,
    UUID paymentId,
    String receiptNumber,
    Double amount,
    String paymentMethod,
    Instant createdAt
) {
    public static PaymentReceiptResponse fromEntity(PaymentReceiptEntity entity) {
        return new PaymentReceiptResponse(
            entity.getId(),
            entity.getPayment().getId(),
            entity.getReceiptNumber(),
            entity.getPayment().getAmount(),
            entity.getPayment().getPaymentMethod().name(),
            entity.getCreatedAt()
        );
    }
}
