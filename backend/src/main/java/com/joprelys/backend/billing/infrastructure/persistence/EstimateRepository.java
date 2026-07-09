package com.joprelys.backend.billing.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import java.util.List;
import java.util.UUID;

public interface EstimateRepository extends JpaRepository<EstimateEntity, UUID> {
    List<EstimateEntity> findByPatientIdOrderByCreatedAtDesc(UUID patientId);
    List<EstimateEntity> findByVisitId(UUID visitId);

    @Query(value = "SELECT nextval('estimate_number_seq')", nativeQuery = true)
    Long getNextEstimateNumberSequenceValue();
}
