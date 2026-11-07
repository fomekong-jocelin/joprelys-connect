package com.joprelys.backend.eco.infrastructure.persistence;

import com.joprelys.backend.eco.domain.EcoStatus;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface EcoPatientLinkRepository extends JpaRepository<EcoPatientLinkEntity, UUID> {

    List<EcoPatientLinkEntity> findByApplicationAndExternalProfileIdAndStatus(
            String application, UUID externalProfileId, EcoStatus status);

    List<EcoPatientLinkEntity> findByPatientIdAndStatusOrderByCreatedAtDesc(UUID patientId, EcoStatus status);
}
