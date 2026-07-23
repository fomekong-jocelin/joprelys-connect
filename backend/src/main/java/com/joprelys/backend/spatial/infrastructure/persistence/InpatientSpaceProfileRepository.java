package com.joprelys.backend.spatial.infrastructure.persistence;

import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface InpatientSpaceProfileRepository extends JpaRepository<InpatientSpaceProfileEntity, UUID> {

    Optional<InpatientSpaceProfileEntity> findBySpaceIdAndOrganizationId(UUID spaceId, UUID organizationId);

    boolean existsBySpaceIdAndOrganizationId(UUID spaceId, UUID organizationId);
}
