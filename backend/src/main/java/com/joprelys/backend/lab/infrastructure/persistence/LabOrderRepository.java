package com.joprelys.backend.lab.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface LabOrderRepository extends JpaRepository<LabOrderEntity, UUID> {

	Optional<LabOrderEntity> findByExamRequestNumber(String examRequestNumber);

	List<LabOrderEntity> findByPatientIdOrderByCreatedAtDesc(UUID patientId);

	List<LabOrderEntity> findAllByOrderByCreatedAtDesc();

	long countByExamRequestNumberStartingWith(String prefix);

	@org.springframework.data.jpa.repository.Query(value = "SELECT * FROM lab_orders WHERE id = :id", nativeQuery = true)
	Optional<LabOrderEntity> findByIdGlobally(@org.springframework.data.repository.query.Param("id") UUID id);
}
