package com.joprelys.backend.spatial.api;

import com.joprelys.backend.spatial.domain.FacilityLocationNodeType;
import com.joprelys.backend.spatial.infrastructure.persistence.FacilityLocationNodeEntity;
import com.joprelys.backend.spatial.infrastructure.persistence.FacilitySpaceEntity;
import com.joprelys.backend.spatial.infrastructure.persistence.InpatientSpaceProfileEntity;
import com.joprelys.backend.spatial.infrastructure.persistence.OrganizationalUnitSpaceAssignmentEntity;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.Instant;
import java.util.UUID;

public final class HospitalLocationDtos {

    private HospitalLocationDtos() {
    }

    public record LocationTypeResponse(String code, int rank) {
    }

    public record FacilityLocationResponse(
            UUID id,
            UUID parentId,
            String code,
            String name,
            FacilityLocationNodeType nodeType,
            boolean active) {

        public static FacilityLocationResponse fromEntity(FacilityLocationNodeEntity entity) {
            return new FacilityLocationResponse(
                    entity.getId(),
                    entity.getParentId(),
                    entity.getCode(),
                    entity.getName(),
                    entity.getNodeType(),
                    entity.isActive());
        }
    }

    public record SaveFacilityLocationRequest(
            UUID parentId,
            @NotBlank @Size(max = 64) String code,
            @NotBlank @Size(max = 120) String name,
            @NotNull FacilityLocationNodeType nodeType) {
    }

    public record SpaceTypeResponse(
            String code,
            String nameFr,
            String nameEn,
            boolean inpatientCompatible) {
    }

    public record FacilitySpaceResponse(
            UUID id,
            UUID locationNodeId,
            String code,
            String name,
            String spaceTypeCode,
            boolean inpatientProfile,
            boolean active) {

        public static FacilitySpaceResponse fromEntity(FacilitySpaceEntity entity, boolean inpatientProfile) {
            return new FacilitySpaceResponse(
                    entity.getId(),
                    entity.getLocationNodeId(),
                    entity.getCode(),
                    entity.getName(),
                    entity.getSpaceTypeCode(),
                    inpatientProfile,
                    entity.isActive());
        }
    }

    public record SaveFacilitySpaceRequest(
            UUID locationNodeId,
            @NotBlank @Size(max = 64) String code,
            @NotBlank @Size(max = 120) String name,
            @NotBlank @Size(max = 64) String spaceTypeCode,
            boolean enableInpatientProfile) {
    }

    public record InpatientProfileResponse(
            UUID spaceId,
            String spaceTypeCode,
            String comfortLevel) {

        public static InpatientProfileResponse fromEntity(InpatientSpaceProfileEntity entity) {
            return new InpatientProfileResponse(
                    entity.getSpaceId(),
                    entity.getSpaceTypeCode(),
                    entity.getComfortLevel());
        }
    }

    public record SaveInpatientProfileRequest(
            @NotBlank @Size(max = 50) String comfortLevel) {
    }

    public record UnitSpaceAssignmentResponse(
            UUID id,
            UUID organizationalUnitId,
            UUID spaceId,
            Instant validFrom,
            Instant validTo) {

        public static UnitSpaceAssignmentResponse fromEntity(OrganizationalUnitSpaceAssignmentEntity entity) {
            return new UnitSpaceAssignmentResponse(
                    entity.getId(),
                    entity.getOrganizationalUnitId(),
                    entity.getSpaceId(),
                    entity.getValidFrom(),
                    entity.getValidTo());
        }
    }

    public record SaveUnitSpaceAssignmentRequest(
            @NotNull UUID organizationalUnitId,
            @NotNull UUID spaceId,
            @NotNull Instant validFrom,
            Instant validTo) {
    }
}
