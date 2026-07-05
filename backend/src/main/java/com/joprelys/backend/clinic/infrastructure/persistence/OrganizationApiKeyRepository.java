package com.joprelys.backend.clinic.infrastructure.persistence;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface OrganizationApiKeyRepository extends JpaRepository<OrganizationApiKeyEntity, UUID> {
	Optional<OrganizationApiKeyEntity> findByHashedKeyAndStatus(String hashedKey, String status);
	List<OrganizationApiKeyEntity> findAllByOrganizationId(UUID organizationId);
}
