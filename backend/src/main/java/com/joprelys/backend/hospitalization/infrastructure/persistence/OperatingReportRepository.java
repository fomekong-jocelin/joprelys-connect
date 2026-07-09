package com.joprelys.backend.hospitalization.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.UUID;

public interface OperatingReportRepository extends JpaRepository<OperatingReportEntity, UUID> {
    List<OperatingReportEntity> findByHospitalizationIdOrderByOperationDateDesc(UUID hospitalizationId);
}
