package com.joprelys.backend.spatial.api;

import com.joprelys.backend.spatial.domain.HospitalServiceType;
import com.joprelys.backend.spatial.infrastructure.persistence.WardEntity;
import java.util.UUID;

public record WardResponse(
        UUID id,
        String name,
        HospitalServiceType serviceType,
        boolean allowsRooms
) {
    public static WardResponse fromEntity(WardEntity entity) {
        return new WardResponse(
                entity.getId(),
                entity.getName(),
                entity.getServiceType(),
                entity.allowsRooms());
    }
}
