package com.joprelys.backend.hospitalization.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.UUID;

public interface HospitalizationNoteRepository extends JpaRepository<HospitalizationNoteEntity, UUID> {
    List<HospitalizationNoteEntity> findByHospitalizationIdOrderByCreatedAtDesc(UUID hospitalizationId);
}
