package com.joprelys.backend.auth.session.infrastructure.persistence;

import com.joprelys.backend.auth.session.application.AuthSessionAuditPort;
import com.joprelys.backend.auth.session.domain.AuthSessionAuditEvent;
import com.joprelys.backend.auth.session.domain.AuthSessionAuditEventType;
import java.util.UUID;
import org.springframework.stereotype.Repository;

@Repository
public class JpaAuthSessionAuditAdapter implements AuthSessionAuditPort {

    private final AuthSessionAuditEventRepository repository;

    public JpaAuthSessionAuditAdapter(AuthSessionAuditEventRepository repository) {
        this.repository = repository;
    }

    @Override
    public void append(AuthSessionAuditEvent event) {
        repository.save(AuthSessionAuditEventEntity.from(event));
    }

    @Override
    public boolean exists(AuthSessionAuditEventType eventType, UUID tokenFamilyId) {
        return repository.existsByEventTypeAndTokenFamilyId(eventType.name(), tokenFamilyId);
    }
}
