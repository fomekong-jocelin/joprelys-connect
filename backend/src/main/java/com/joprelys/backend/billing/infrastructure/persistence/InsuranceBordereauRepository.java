package com.joprelys.backend.billing.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface InsuranceBordereauRepository extends JpaRepository<InsuranceBordereauEntity, UUID> {

    @Query(value = "SELECT nextval('insurance_bordereau_number_seq')", nativeQuery = true)
    Long getNextBordereauNumberSequenceValue();

    List<InsuranceBordereauEntity> findByOrganizationIdOrderByCreatedAtDesc(UUID organizationId);

    Optional<InsuranceBordereauEntity> findByIdAndOrganizationId(UUID id, UUID organizationId);

    List<InsuranceBordereauEntity> findByCreatedAtBetweenOrderByCreatedAtDesc(Instant start, Instant end);
}
