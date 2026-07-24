package com.joprelys.backend.auth.api;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public final class StaffAssignmentDtos {

    private StaffAssignmentDtos() {
    }

    public record AssignmentRoleResponse(
            String code,
            String nameFr,
            String nameEn) {
    }

    public record SpecialtyAssignmentRequest(
            @NotBlank String specialtyCode,
            boolean primary,
            @NotNull Instant validFrom,
            Instant validTo) {
    }

    public record UnitAssignmentRequest(
            @NotNull UUID organizationalUnitId,
            @NotBlank String assignmentRoleCode,
            boolean primary,
            @NotNull Instant validFrom,
            Instant validTo) {
    }

    public record CloseAssignmentRequest(
            @NotNull Instant closedAt) {
    }

    public record SpecialtyAssignmentResponse(
            UUID id,
            String specialtyCode,
            boolean primary,
            Instant validFrom,
            Instant validTo,
            boolean active) {
    }

    public record UnitAssignmentResponse(
            UUID id,
            UUID organizationalUnitId,
            String assignmentRoleCode,
            boolean primary,
            Instant validFrom,
            Instant validTo,
            boolean active) {
    }

    public record StructureResponse(
            List<SpecialtyAssignmentResponse> specialties,
            List<UnitAssignmentResponse> unitAssignments) {
    }
}
