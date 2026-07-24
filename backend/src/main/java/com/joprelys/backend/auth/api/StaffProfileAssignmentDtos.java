package com.joprelys.backend.auth.api;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public final class StaffProfileAssignmentDtos {

    private StaffProfileAssignmentDtos() {
    }

    public record ActiveSpecialtyResponse(
            String specialtyCode,
            String nameFr,
            String nameEn,
            boolean primary,
            Instant validFrom,
            Instant validTo) {
    }

    public record ActiveUnitResponse(
            UUID organizationalUnitId,
            String unitCode,
            String nameFr,
            String nameEn,
            String assignmentRoleCode,
            String assignmentRoleNameFr,
            String assignmentRoleNameEn,
            boolean primary,
            Instant validFrom,
            Instant validTo) {
    }

    public record ActiveStructureResponse(
            List<ActiveSpecialtyResponse> specialties,
            List<ActiveUnitResponse> unitAssignments) {
    }
}
