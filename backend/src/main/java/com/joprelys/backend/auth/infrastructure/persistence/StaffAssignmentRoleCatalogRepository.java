package com.joprelys.backend.auth.infrastructure.persistence;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface StaffAssignmentRoleCatalogRepository extends JpaRepository<StaffAssignmentRoleCatalogEntity, String> {

    List<StaffAssignmentRoleCatalogEntity> findAllByActiveTrueOrderByNameFrAsc();
}
