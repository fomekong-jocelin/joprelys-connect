package com.joprelys.backend.billing.api;

import com.joprelys.backend.billing.infrastructure.persistence.PatientDepositEntity;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record DepositResponse(
        UUID id,
        String depositNumber,
        UUID patientId,
        UUID visitId,
        BigDecimal amount,
        BigDecimal remainingAmount,
        String method,
        String reference,
        String note,
        Instant createdAt
) {
    public static DepositResponse fromEntity(PatientDepositEntity entity) {
        return new DepositResponse(entity.getId(), entity.getDepositNumber(), entity.getPatientId(), entity.getVisitId(),
                entity.getAmount(), entity.getRemainingAmount(), entity.getPaymentMethod().name(),
                entity.getReferenceNumber(), entity.getNote(), entity.getCreatedAt());
    }
}
