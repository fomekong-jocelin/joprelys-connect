package com.joprelys.backend.billing.api;

import com.joprelys.backend.billing.infrastructure.persistence.PaymentEntity;
import com.joprelys.backend.billing.infrastructure.persistence.PaymentMethod;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record PaymentResponse(
        UUID id,
        UUID invoiceId,
        BigDecimal amount,
        PaymentMethod paymentMethod,
        String referenceNumber,
        UUID receivedByUserId,
        Instant createdAt
) {
    public static PaymentResponse fromEntity(PaymentEntity entity) {
        return new PaymentResponse(
                entity.getId(),
                entity.getInvoice().getId(),
                entity.getAmount(),
                entity.getPaymentMethod(),
                entity.getReferenceNumber(),
                entity.getReceivedByUserId(),
                entity.getCreatedAt()
        );
    }
}
