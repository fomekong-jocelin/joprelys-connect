package com.joprelys.backend.lab.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.UUID;

@Repository
public interface LabResultRepository extends JpaRepository<LabResultEntity, UUID> {

	List<LabResultEntity> findByPatientIdOrderByCreatedAtDesc(UUID patientId);

	List<LabResultEntity> findByLabOrderId(UUID labOrderId);

	long countByResultNumberStartingWith(String prefix);
}
