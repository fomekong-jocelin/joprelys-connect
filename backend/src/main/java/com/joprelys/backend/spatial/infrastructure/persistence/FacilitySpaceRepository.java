package com.joprelys.backend.spatial.infrastructure.persistence;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface FacilitySpaceRepository extends JpaRepository<FacilitySpaceEntity, UUID> {

    List<FacilitySpaceEntity> findAllByOrganizationIdOrderByCodeAsc(UUID organizationId);

    List<FacilitySpaceEntity> findAllByOrganizationIdAndActiveTrueOrderByCodeAsc(UUID organizationId);

    List<FacilitySpaceEntity> findAllByOrganizationIdAndLocationNodeIdOrderByCodeAsc(
            UUID organizationId,
            UUID locationNodeId);

    Optional<FacilitySpaceEntity> findByIdAndOrganizationId(UUID id, UUID organizationId);

    boolean existsByOrganizationIdAndCodeIgnoreCase(UUID organizationId, String code);

    boolean existsByOrganizationIdAndCodeIgnoreCaseAndIdNot(UUID organizationId, String code, UUID id);

    boolean existsByOrganizationIdAndLocationNodeIdAndActiveTrue(UUID organizationId, UUID locationNodeId);
}
