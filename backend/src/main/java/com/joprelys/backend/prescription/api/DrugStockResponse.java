package com.joprelys.backend.prescription.api;

import com.joprelys.backend.prescription.infrastructure.persistence.DrugStockEntity;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

/**
 * DTO de réponse pour les stocks de médicaments (STORY-1103).
 */
public record DrugStockResponse(
        UUID id,
        String drugName,
        String genericName,
        String unit,
        int quantityAvailable,
        int minimumThreshold,
        boolean belowThreshold,
        String batchNumber,
        LocalDate expiryDate,
        String supplier,
        Instant updatedAt
) {
    public static DrugStockResponse fromEntity(DrugStockEntity e) {
        return new DrugStockResponse(
                e.getId(), e.getDrugName(), e.getGenericName(), e.getUnit(),
                e.getQuantityAvailable(), e.getMinimumThreshold(), e.isBelowThreshold(),
                e.getBatchNumber(), e.getExpiryDate(), e.getSupplier(), e.getUpdatedAt()
        );
    }
}
