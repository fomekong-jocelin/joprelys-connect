package com.joprelys.backend.eco.infrastructure.persistence;

import com.joprelys.backend.eco.domain.EcoStatus;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface EcoConsentRepository extends JpaRepository<EcoConsentEntity, UUID> {

    Optional<EcoConsentEntity> findFirstByPatientLinkIdAndStatus(UUID patientLinkId, EcoStatus status);
}
