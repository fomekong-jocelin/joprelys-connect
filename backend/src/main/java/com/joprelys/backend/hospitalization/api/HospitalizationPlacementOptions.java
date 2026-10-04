package com.joprelys.backend.hospitalization.api;

import com.joprelys.backend.hospitalorganization.api.HospitalServiceCatalogResponse;
import com.joprelys.backend.hospitalorganization.api.OrganizationalUnitResponse;
import com.joprelys.backend.spatial.api.HospitalLocationDtos.FacilitySpaceResponse;
import com.joprelys.backend.spatial.api.HospitalLocationDtos.UnitSpaceAssignmentResponse;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record HospitalizationPlacementOptions(
        List<OrganizationalUnitResponse> units,
        List<HospitalServiceCatalogResponse> serviceCatalog,
        List<FacilitySpaceResponse> spaces,
        List<UnitSpaceAssignmentResponse> assignments,
        List<PractitionerOption> staff) {
    public record UnitOption(UUID id) {}
    public record PractitionerOption(UUID id, String displayName, String role, boolean enabled,
                                    List<UnitOption> activeOrganizationalUnits) {}
    public record AdmissionVisit(UUID id, String visitNumber, String reason, Instant createdAt, String status) {}
}
