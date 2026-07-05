package com.joprelys.backend.visit.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.List;
import java.util.UUID;

public interface VisitRepository extends JpaRepository<VisitEntity, UUID> {

	@Query("SELECT v FROM VisitEntity v JOIN FETCH v.patient LEFT JOIN FETCH v.vitals WHERE v.status = 'EN_COURS' ORDER BY v.createdAt ASC")
	List<VisitEntity> findActiveVisits();

	boolean existsByPatientIdAndStatus(UUID patientId, String status);

	@Query(value = "SELECT * FROM visits WHERE id = :id", nativeQuery = true)
	java.util.Optional<VisitEntity> findByIdGlobally(@Param("id") UUID id);
}

