package com.joprelys.backend.prescription.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.UUID;

public interface PrescriptionDispensationRepository extends JpaRepository<PrescriptionDispensationEntity, UUID> {
	List<PrescriptionDispensationEntity> findByPrescriptionIdOrderByDispensedAtDesc(UUID prescriptionId);
}
