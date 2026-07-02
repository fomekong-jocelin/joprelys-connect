package com.joprelys.backend.visit.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import java.util.List;
import java.util.UUID;

public interface VisitRepository extends JpaRepository<VisitEntity, UUID> {

	@Query("SELECT v FROM VisitEntity v JOIN FETCH v.patient WHERE v.status = 'EN_COURS' ORDER BY v.createdAt ASC")
	List<VisitEntity> findActiveVisits();

	boolean existsByPatientIdAndStatus(UUID patientId, String status);
}
