package com.joprelys.backend.hospitalorganization.infrastructure.persistence;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OrganizationalUnitRepository extends JpaRepository<OrganizationalUnitEntity, UUID> {

    List<OrganizationalUnitEntity> findAllByOrderByCodeAsc();

    List<OrganizationalUnitEntity> findAllByActiveTrueOrderByCodeAsc();

    Optional<OrganizationalUnitEntity> findByIdAndOrganizationId(UUID id, UUID organizationId);

    boolean existsByCodeIgnoreCase(String code);

    boolean existsByCodeIgnoreCaseAndIdNot(String code, UUID id);

    boolean existsByParentIdAndActiveTrue(UUID parentId);
}
