package com.joprelys.backend.hospitalization.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.UUID;

public interface SurgicalImplantRepository extends JpaRepository<SurgicalImplantEntity, UUID> {
    List<SurgicalImplantEntity> findByHospitalizationId(UUID hospitalizationId);
}
