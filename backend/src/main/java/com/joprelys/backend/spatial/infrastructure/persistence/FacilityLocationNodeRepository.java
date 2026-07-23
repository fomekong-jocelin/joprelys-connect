package com.joprelys.backend.spatial.infrastructure.persistence;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface FacilityLocationNodeRepository extends JpaRepository<FacilityLocationNodeEntity, UUID> {

    List<FacilityLocationNodeEntity> findAllByOrganizationIdOrderByCodeAsc(UUID organizationId);

    List<FacilityLocationNodeEntity> findAllByOrganizationIdAndActiveTrueOrderByCodeAsc(UUID organizationId);

    Optional<FacilityLocationNodeEntity> findByIdAndOrganizationId(UUID id, UUID organizationId);

    boolean existsByOrganizationIdAndCodeIgnoreCase(UUID organizationId, String code);

    boolean existsByOrganizationIdAndCodeIgnoreCaseAndIdNot(UUID organizationId, String code, UUID id);

    boolean existsByOrganizationIdAndParentIdAndActiveTrue(UUID organizationId, UUID parentId);
}
