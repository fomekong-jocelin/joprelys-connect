package com.joprelys.backend.auth.infrastructure.persistence;

import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface StaffOrganizationalUnitAssignmentRepository
        extends JpaRepository<StaffOrganizationalUnitAssignmentEntity, UUID> {

    List<StaffOrganizationalUnitAssignmentEntity> findAllByOrganizationIdAndStaffIdOrderByValidFromDesc(
            UUID organizationId,
            UUID staffId);
}
