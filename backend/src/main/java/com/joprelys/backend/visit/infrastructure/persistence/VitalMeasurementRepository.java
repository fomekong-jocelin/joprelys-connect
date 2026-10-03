package com.joprelys.backend.visit.infrastructure.persistence;

import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface VitalMeasurementRepository extends JpaRepository<VitalMeasurementEntity, UUID> {

	List<VitalMeasurementEntity> findByVisitIdOrderByRecordedAtDesc(UUID visitId);
}
