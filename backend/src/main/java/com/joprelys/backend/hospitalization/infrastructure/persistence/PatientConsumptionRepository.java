package com.joprelys.backend.hospitalization.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.UUID;

public interface PatientConsumptionRepository extends JpaRepository<PatientConsumptionEntity, UUID> {
    List<PatientConsumptionEntity> findByHospitalizationIdOrderByConsumedAtDesc(UUID hospitalizationId);
}
