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

	@Query(value = "SELECT * FROM prescriptions WHERE id = :id", nativeQuery = true)
	Optional<PrescriptionEntity> findByIdGlobally(@Param("id") UUID id);

	@Query(value = "SELECT * FROM prescriptions WHERE consultation_id = :consultationId", nativeQuery = true)
	Optional<PrescriptionEntity> findByConsultationIdGlobally(@Param("consultationId") UUID consultationId);

	@Query(value = "SELECT v.patient_id FROM prescriptions p JOIN consultations c ON p.consultation_id = c.id JOIN visits v ON c.visit_id = v.id WHERE p.id = :prescriptionId", nativeQuery = true)
	Optional<Object> findPatientIdByPrescriptionId(@Param("prescriptionId") UUID prescriptionId);

	@Query("SELECT p FROM PrescriptionEntity p LEFT JOIN FETCH p.items WHERE p.consultation.visit.patient.id = :patientId AND p.status = 'ACTIVE' ORDER BY p.createdAt DESC")
	java.util.List<PrescriptionEntity> findActivePrescriptionsByPatientId(@Param("patientId") java.util.UUID patientId);

	@Query("SELECT p FROM PrescriptionEntity p JOIN FETCH p.consultation LEFT JOIN FETCH p.items WHERE p.id = :id")
	Optional<PrescriptionEntity> findByIdWithConsultationAndItems(@Param("id") UUID id);

	@Query("SELECT p FROM PrescriptionEntity p WHERE p.status = 'ACTIVE'")
	java.util.List<PrescriptionEntity> findAllActivePrescriptions();
}
