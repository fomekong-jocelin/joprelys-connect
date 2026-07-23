package com.joprelys.backend.hospitalorganization.infrastructure.persistence;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OrganizationalUnitRepository extends JpaRepository<OrganizationalUnitEntity, UUID> {

    List<OrganizationalUnitEntity> findAllByOrganizationIdOrderByCodeAsc(UUID organizationId);

    List<OrganizationalUnitEntity> findAllByOrganizationIdAndActiveTrueOrderByCodeAsc(UUID organizationId);

    Optional<OrganizationalUnitEntity> findByIdAndOrganizationId(UUID id, UUID organizationId);

    boolean existsByOrganizationIdAndCodeIgnoreCase(UUID organizationId, String code);

    boolean existsByOrganizationIdAndCodeIgnoreCaseAndIdNot(UUID organizationId, String code, UUID id);

    boolean existsByOrganizationIdAndParentIdAndActiveTrue(UUID organizationId, UUID parentId);
}
