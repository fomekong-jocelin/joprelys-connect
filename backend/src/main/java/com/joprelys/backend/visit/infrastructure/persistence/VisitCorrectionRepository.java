package com.joprelys.backend.visit.infrastructure.persistence;

import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface VisitCorrectionRepository extends JpaRepository<VisitCorrectionEntity, UUID> {

	List<VisitCorrectionEntity> findByVisitIdOrderByCreatedAtDesc(UUID visitId);
}
