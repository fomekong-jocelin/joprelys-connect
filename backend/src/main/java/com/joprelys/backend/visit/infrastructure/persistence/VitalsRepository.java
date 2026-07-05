package com.joprelys.backend.visit.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface VitalsRepository extends JpaRepository<VitalsEntity, UUID> {

	Optional<VitalsEntity> findByVisitId(UUID visitId);

	boolean existsByVisitId(UUID visitId);

	@Query("SELECT v FROM VitalsEntity v JOIN FETCH v.visit vi JOIN FETCH vi.patient p WHERE p.id = :patientId ORDER BY v.createdAt DESC")
	List<VitalsEntity> findAllByPatientId(@Param("patientId") UUID patientId);
}

