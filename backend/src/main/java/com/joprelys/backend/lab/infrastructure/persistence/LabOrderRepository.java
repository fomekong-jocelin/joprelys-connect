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

	long countByExamRequestNumberStartingWith(String prefix);
}
