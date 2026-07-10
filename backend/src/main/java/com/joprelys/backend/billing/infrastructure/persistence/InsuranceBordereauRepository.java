package com.joprelys.backend.billing.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.UUID;

@Repository
public interface InsuranceBordereauRepository extends JpaRepository<InsuranceBordereauEntity, UUID> {

    @Query(value = "SELECT nextval('insurance_bordereau_number_seq')", nativeQuery = true)
    Long getNextBordereauNumberSequenceValue();

    List<InsuranceBordereauEntity> findByOrganizationIdOrderByCreatedAtDesc(UUID organizationId);
    List<InsuranceBordereauEntity> findByCreatedAtBetweenOrderByCreatedAtDesc(java.time.Instant start, java.time.Instant end);
}
