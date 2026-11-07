package com.joprelys.backend.eco.infrastructure.persistence;

import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.data.domain.Limit;
import org.springframework.data.jpa.repository.JpaRepository;

public interface EcoOutboxRepository extends JpaRepository<EcoOutboxEntity, UUID> {

    List<EcoOutboxEntity> findByStatusAndNextAttemptAtLessThanEqualOrderByNextAttemptAtAsc(
            EcoOutboxEntity.Status status, Instant now, Limit limit);
}
