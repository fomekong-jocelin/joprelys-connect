package com.joprelys.backend.hospitalization.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.UUID;

public interface HospitalizationDailyCareRepository extends JpaRepository<HospitalizationDailyCareEntity, UUID> {
    List<HospitalizationDailyCareEntity> findByHospitalizationIdOrderByPerformedAtDesc(UUID hospitalizationId);
}
