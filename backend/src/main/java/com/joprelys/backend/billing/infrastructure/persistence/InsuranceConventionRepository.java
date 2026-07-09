package com.joprelys.backend.billing.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.UUID;

public interface InsuranceConventionRepository extends JpaRepository<InsuranceConventionEntity, UUID> {
}
