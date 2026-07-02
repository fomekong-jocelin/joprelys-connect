package com.joprelys.backend.visit.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;
import java.util.UUID;

public interface VitalsRepository extends JpaRepository<VitalsEntity, UUID> {

	Optional<VitalsEntity> findByVisitId(UUID visitId);

	boolean existsByVisitId(UUID visitId);
}
