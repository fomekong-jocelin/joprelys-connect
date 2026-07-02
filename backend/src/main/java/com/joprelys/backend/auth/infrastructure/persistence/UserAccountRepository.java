package com.joprelys.backend.auth.infrastructure.persistence;

import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserAccountRepository extends JpaRepository<UserAccountEntity, UUID> {

	Optional<UserAccountEntity> findByEmail(String email);

	boolean existsByEmail(String email);
}
