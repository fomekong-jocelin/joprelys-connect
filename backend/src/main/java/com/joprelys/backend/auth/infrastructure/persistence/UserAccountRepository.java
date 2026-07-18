package com.joprelys.backend.auth.infrastructure.persistence;

import jakarta.persistence.LockModeType;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface UserAccountRepository extends JpaRepository<UserAccountEntity, UUID> {

	Optional<UserAccountEntity> findByEmail(String email);

	Optional<UserAccountEntity> findByEmailIgnoreCase(String email);

	boolean existsByEmail(String email);

	List<UserAccountEntity> findAllByOrganizationId(UUID organizationId);

	List<UserAccountEntity> findAllByOrganizationIdAndIdNotOrderByDisplayNameAsc(UUID organizationId, UUID id);

	Optional<UserAccountEntity> findByIdAndOrganizationId(UUID id, UUID organizationId);

	List<UserAccountEntity> findAllByOrganizationIdAndEnabledTrueOrderByDisplayNameAsc(UUID organizationId);

	@Lock(LockModeType.PESSIMISTIC_WRITE)
	@Query("select user from UserAccountEntity user where user.id = :id and user.organizationId = :organizationId")
	Optional<UserAccountEntity> findByIdAndOrganizationIdForUpdate(
			@Param("id") UUID id,
			@Param("organizationId") UUID organizationId);
}
