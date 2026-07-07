package com.joprelys.backend.patient.infrastructure.persistence;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.time.Instant;
import java.util.UUID;

@Repository
public interface PatientPreRegistrationRepository extends JpaRepository<PatientPreRegistrationEntity, UUID> {

    Page<PatientPreRegistrationEntity> findAllByStatus(PreRegistrationStatus status, Pageable pageable);

    void deleteByStatusAndCreatedAtBefore(PreRegistrationStatus status, Instant limit);
}
