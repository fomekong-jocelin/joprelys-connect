package com.joprelys.backend.visit.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;
import java.util.UUID;

public interface MedicalDocumentRepository extends JpaRepository<MedicalDocumentEntity, UUID> {
    Optional<MedicalDocumentEntity> findByVisitId(UUID visitId);
}
