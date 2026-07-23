package com.joprelys.backend.hospitalorganization.api;

import com.joprelys.backend.hospitalorganization.domain.OrganizationalUnitType;
import com.joprelys.backend.hospitalorganization.infrastructure.persistence.OrganizationalUnitEntity;
import java.util.UUID;

public record OrganizationalUnitResponse(
        UUID id,
        UUID parentId,
        String code,
        String name,
        OrganizationalUnitType unitType,
        String serviceCatalogCode,
        boolean active) {

    public static OrganizationalUnitResponse fromEntity(OrganizationalUnitEntity entity) {
        return new OrganizationalUnitResponse(
                entity.getId(),
                entity.getParentId(),
                entity.getCode(),
                entity.getName(),
                entity.getUnitType(),
                entity.getServiceCatalogCode(),
                entity.isActive());
    }
}
