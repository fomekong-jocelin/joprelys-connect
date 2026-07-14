package com.joprelys.backend.auth.session.infrastructure.persistence;

import java.time.Instant;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;

public interface AuthSessionAuditEventRepository extends JpaRepository<AuthSessionAuditEventEntity, UUID> {

    boolean existsByEventTypeAndTokenFamilyId(String eventType, UUID tokenFamilyId);

    @Modifying
    long deleteByOccurredAtBefore(Instant cutoff);
}
