package com.joprelys.backend.auth.session.infrastructure.persistence;

import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AuthSessionAuditEventRepository extends JpaRepository<AuthSessionAuditEventEntity, UUID> {

    boolean existsByEventTypeAndTokenFamilyId(String eventType, UUID tokenFamilyId);
}
