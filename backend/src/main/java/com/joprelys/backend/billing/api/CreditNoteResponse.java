package com.joprelys.backend.billing.api;

import com.joprelys.backend.billing.infrastructure.persistence.CreditNoteEntity;
import java.time.Instant;
import java.util.UUID;

public record CreditNoteResponse(
    UUID id,
    UUID invoiceId,
    String creditNoteNumber,
    Double amount,
    String reason,
    String status,
    Instant createdAt
) {
    public static CreditNoteResponse fromEntity(CreditNoteEntity entity) {
        return new CreditNoteResponse(
            entity.getId(), entity.getInvoiceId(),
            entity.getCreditNoteNumber(), entity.getAmount(),
            entity.getReason(), entity.getStatus(),
            entity.getCreatedAt()
        );
    }
}
