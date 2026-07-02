package com.joprelys.backend.prescription.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.Optional;
import java.util.UUID;

public interface PrescriptionRepository extends JpaRepository<PrescriptionEntity, UUID> {

	@Query("SELECT p FROM PrescriptionEntity p WHERE p.consultation.id = :consultationId")
	Optional<PrescriptionEntity> findByConsultationId(@Param("consultationId") UUID consultationId);

	boolean existsByConsultationId(UUID consultationId);
}
