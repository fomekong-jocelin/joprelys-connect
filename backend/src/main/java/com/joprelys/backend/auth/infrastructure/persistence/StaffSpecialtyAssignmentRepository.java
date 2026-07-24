package com.joprelys.backend.auth.infrastructure.persistence;

import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface StaffSpecialtyAssignmentRepository extends JpaRepository<StaffSpecialtyAssignmentEntity, UUID> {

    List<StaffSpecialtyAssignmentEntity> findAllByOrganizationIdAndStaffIdOrderByValidFromDesc(
            UUID organizationId,
            UUID staffId);

    List<StaffSpecialtyAssignmentEntity> findAllByOrganizationIdAndStaffIdAndValidFromLessThanOrderByValidFromDesc(
            UUID organizationId,
            UUID staffId,
            Instant before);
}
