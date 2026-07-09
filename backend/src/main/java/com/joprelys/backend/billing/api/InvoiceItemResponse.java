package com.joprelys.backend.billing.api;

import com.joprelys.backend.billing.infrastructure.persistence.InvoiceItemEntity;
import com.joprelys.backend.billing.infrastructure.persistence.InvoiceItemType;
import java.util.UUID;

public record InvoiceItemResponse(
        UUID id,
        String label,
        InvoiceItemType itemType,
        Double unitPrice,
        Double quantity,
        Double coefficient,
        Double totalItemAmount
) {
    public static InvoiceItemResponse fromEntity(InvoiceItemEntity entity) {
        return new InvoiceItemResponse(
                entity.getId(),
                entity.getLabel(),
                entity.getItemType(),
                entity.getUnitPrice(),
                entity.getQuantity(),
                entity.getCoefficient(),
                entity.getTotalItemAmount()
        );
    }
}
