package com.joprelys.backend.consultation.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ConsultationRepository extends JpaRepository<ConsultationEntity, UUID> {

	@Query("SELECT c FROM ConsultationEntity c WHERE c.visit.id = :visitId")
	Optional<ConsultationEntity> findByVisitId(@Param("visitId") UUID visitId);

	boolean existsByVisitId(UUID visitId);

	@Query("SELECT COUNT(c) FROM ConsultationEntity c WHERE YEAR(c.createdAt) = YEAR(CURRENT_TIMESTAMP)")
	long countThisYear();

	@Query("SELECT c FROM ConsultationEntity c JOIN FETCH c.visit JOIN FETCH c.doctor WHERE c.visit.patient.id = :patientId ORDER BY c.createdAt DESC")
	List<ConsultationEntity> findByPatientIdOrderByCreatedAtDesc(@Param("patientId") UUID patientId);
}
