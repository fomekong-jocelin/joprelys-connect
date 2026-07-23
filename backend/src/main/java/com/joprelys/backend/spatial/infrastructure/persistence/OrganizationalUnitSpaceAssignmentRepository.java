package com.joprelys.backend.spatial.infrastructure.persistence;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface OrganizationalUnitSpaceAssignmentRepository
        extends JpaRepository<OrganizationalUnitSpaceAssignmentEntity, UUID> {

    Optional<OrganizationalUnitSpaceAssignmentEntity> findByIdAndOrganizationId(UUID id, UUID organizationId);

    List<OrganizationalUnitSpaceAssignmentEntity> findAllByOrganizationIdOrderByValidFromDesc(UUID organizationId);

    List<OrganizationalUnitSpaceAssignmentEntity> findAllByOrganizationIdAndSpaceIdOrderByValidFromDesc(
            UUID organizationId,
            UUID spaceId);

    List<OrganizationalUnitSpaceAssignmentEntity> findAllByOrganizationIdAndOrganizationalUnitIdOrderByValidFromDesc(
            UUID organizationId,
            UUID organizationalUnitId);

    @Query("""
            SELECT COUNT(a) > 0
            FROM OrganizationalUnitSpaceAssignmentEntity a
            WHERE a.organizationId = :organizationId
              AND a.organizationalUnitId = :unitId
              AND a.spaceId = :spaceId
              AND a.validFrom <= :at
              AND (a.validTo IS NULL OR a.validTo > :at)
            """)
    boolean existsActiveAt(
            @Param("organizationId") UUID organizationId,
            @Param("unitId") UUID unitId,
            @Param("spaceId") UUID spaceId,
            @Param("at") Instant at);

    @Query("""
            SELECT COUNT(a) > 0
            FROM OrganizationalUnitSpaceAssignmentEntity a
            WHERE a.organizationId = :organizationId
              AND a.organizationalUnitId = :unitId
              AND a.spaceId = :spaceId
              AND (:excludedId IS NULL OR a.id <> :excludedId)
              AND a.validFrom < COALESCE(:validTo, :infinity)
              AND COALESCE(a.validTo, :infinity) > :validFrom
            """)
    boolean hasOverlap(
            @Param("organizationId") UUID organizationId,
            @Param("unitId") UUID unitId,
            @Param("spaceId") UUID spaceId,
            @Param("excludedId") UUID excludedId,
            @Param("validFrom") Instant validFrom,
            @Param("validTo") Instant validTo,
            @Param("infinity") Instant infinity);
}
