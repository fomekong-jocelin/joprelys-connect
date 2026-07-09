package com.joprelys.backend.hospitalization.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.UUID;

public interface MedicationAdministrationRepository extends JpaRepository<MedicationAdministrationEntity, UUID> {
    List<MedicationAdministrationEntity> findByHospitalizationIdOrderByAdministeredAtDesc(UUID hospitalizationId);
}
