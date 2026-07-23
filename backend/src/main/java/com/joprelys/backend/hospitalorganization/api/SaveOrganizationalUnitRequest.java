package com.joprelys.backend.hospitalorganization.api;

import com.joprelys.backend.hospitalorganization.domain.OrganizationalUnitType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.UUID;

public record SaveOrganizationalUnitRequest(
        @NotBlank @Size(max = 64) String code,
        @NotNull OrganizationalUnitType unitType,
        UUID parentId,
        @Size(max = 120) String name,
        @Size(max = 64) String serviceCatalogCode) {
}
