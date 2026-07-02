package com.joprelys.backend.clinic.infrastructure.persistence;

import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface OrganizationRepository extends JpaRepository<OrganizationEntity, UUID> {
	Optional<OrganizationEntity> findByEmail(String email);
	boolean existsByEmail(String email);
}
