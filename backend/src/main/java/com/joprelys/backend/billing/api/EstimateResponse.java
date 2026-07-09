package com.joprelys.backend.billing.api;

import com.joprelys.backend.billing.infrastructure.persistence.EstimateEntity;
import com.joprelys.backend.billing.infrastructure.persistence.EstimateItemEntity;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record EstimateResponse(
    UUID id,
    UUID patientId,
    UUID visitId,
    String estimateNumber,
    Double totalAmount,
    Double patientShare,
    Double insuranceShare,
    String status,
    List<EstimateItemResponse> items,
    Instant createdAt,
    Instant updatedAt
) {
    public record EstimateItemResponse(
        UUID id,
        String label,
        String itemType,
        Double unitPrice,
        Double quantity,
        Double totalAmount
    ) {
        public static EstimateItemResponse fromEntity(EstimateItemEntity e) {
            return new EstimateItemResponse(
                e.getId(), e.getLabel(), e.getItemType(),
                e.getUnitPrice(), e.getQuantity(),
                e.getUnitPrice() * e.getQuantity()
            );
        }
    }

    public static EstimateResponse fromEntity(EstimateEntity entity) {
        List<EstimateItemResponse> items = entity.getItems() != null
            ? entity.getItems().stream().map(EstimateItemResponse::fromEntity).toList()
            : List.of();
        return new EstimateResponse(
            entity.getId(), entity.getPatientId(), entity.getVisitId(),
            entity.getEstimateNumber(), entity.getTotalAmount(),
            entity.getPatientShare(), entity.getInsuranceShare(),
            entity.getStatus(), items,
            entity.getCreatedAt(), entity.getUpdatedAt()
        );
    }
}
