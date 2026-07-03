package com.joprelys.backend.prescription.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.Optional;
import java.util.UUID;

public interface PrescriptionRepository extends JpaRepository<PrescriptionEntity, UUID> {

	@Query("SELECT p FROM PrescriptionEntity p LEFT JOIN FETCH p.items WHERE p.consultation.id = :consultationId")
	Optional<PrescriptionEntity> findByConsultationId(@Param("consultationId") UUID consultationId);

	@Query("SELECT p FROM PrescriptionEntity p LEFT JOIN FETCH p.items WHERE p.prescriptionNumber = :prescriptionNumber")
	Optional<PrescriptionEntity> findByPrescriptionNumber(@Param("prescriptionNumber") String prescriptionNumber);

	@Query(value = "SELECT * FROM prescriptions WHERE prescription_number = :prescriptionNumber", nativeQuery = true)
	Optional<PrescriptionEntity> findByPrescriptionNumberGlobally(@Param("prescriptionNumber") String prescriptionNumber);

	boolean existsByConsultationId(UUID consultationId);
}
