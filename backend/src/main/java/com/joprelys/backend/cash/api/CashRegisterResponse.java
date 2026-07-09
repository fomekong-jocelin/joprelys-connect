package com.joprelys.backend.cash.api;

import com.joprelys.backend.cash.infrastructure.persistence.CashRegisterEntity;
import java.util.UUID;

public record CashRegisterResponse(
    UUID id,
    String code,
    String name
) {
    public static CashRegisterResponse fromEntity(CashRegisterEntity entity) {
        return new CashRegisterResponse(
            entity.getId(),
            entity.getCode(),
            entity.getName()
        );
    }
}
