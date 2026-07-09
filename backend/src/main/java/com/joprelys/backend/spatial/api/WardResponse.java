package com.joprelys.backend.spatial.api;

import com.joprelys.backend.spatial.infrastructure.persistence.WardEntity;
import java.util.UUID;

public record WardResponse(
        UUID id,
        String name
) {
    public static WardResponse fromEntity(WardEntity entity) {
        return new WardResponse(entity.getId(), entity.getName());
    }
}
