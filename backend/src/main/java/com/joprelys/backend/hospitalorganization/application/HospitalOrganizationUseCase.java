package com.joprelys.backend.hospitalorganization.application;

import com.joprelys.backend.hospitalorganization.api.HospitalServiceCatalogResponse;
import com.joprelys.backend.hospitalorganization.api.MedicalSpecialtyCatalogResponse;
import com.joprelys.backend.hospitalorganization.api.OrganizationalUnitResponse;
import com.joprelys.backend.hospitalorganization.api.SaveOrganizationalUnitRequest;
import java.util.List;
import java.util.UUID;

public interface HospitalOrganizationUseCase {

    List<HospitalServiceCatalogResponse> listServiceCatalog();

    List<MedicalSpecialtyCatalogResponse> listSpecialtyCatalog();

    List<OrganizationalUnitResponse> listUnits(UUID requestedOrganizationId, boolean includeInactive);

    OrganizationalUnitResponse createUnit(UUID requestedOrganizationId, SaveOrganizationalUnitRequest request);

    OrganizationalUnitResponse updateUnit(
            UUID requestedOrganizationId,
            UUID unitId,
            SaveOrganizationalUnitRequest request);

    OrganizationalUnitResponse setUnitActive(UUID requestedOrganizationId, UUID unitId, boolean active);
}
