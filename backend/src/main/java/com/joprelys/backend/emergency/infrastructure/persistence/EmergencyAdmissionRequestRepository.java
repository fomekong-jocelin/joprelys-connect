package com.joprelys.backend.emergency.infrastructure.persistence;

import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface EmergencyAdmissionRequestRepository
        extends JpaRepository<EmergencyAdmissionRequestEntity, UUID> {
}
