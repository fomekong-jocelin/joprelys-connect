package com.joprelys.backend.hospitalization.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.UUID;

public interface SurgicalConsentRepository extends JpaRepository<SurgicalConsentEntity, UUID> {
    List<SurgicalConsentEntity> findByHospitalizationId(UUID hospitalizationId);
}
