package com.joprelys.backend.auth.infrastructure.persistence;

import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserAccountRepository extends JpaRepository<UserAccountEntity, UUID> {

	Optional<UserAccountEntity> findByEmail(String email);

	boolean existsByEmail(String email);

	java.util.List<UserAccountEntity> findAllByOrganizationId(UUID organizationId);

	java.util.List<UserAccountEntity> findAllByOrganizationIdAndIdNotOrderByDisplayNameAsc(UUID organizationId, UUID id);

	Optional<UserAccountEntity> findByIdAndOrganizationId(UUID id, UUID organizationId);
}
