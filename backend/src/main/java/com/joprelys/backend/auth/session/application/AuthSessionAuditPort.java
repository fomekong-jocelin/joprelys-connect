package com.joprelys.backend.auth.session.application;

import com.joprelys.backend.auth.session.domain.AuthSessionAuditEvent;
import com.joprelys.backend.auth.session.domain.AuthSessionAuditEventType;
import java.util.UUID;

public interface AuthSessionAuditPort {

    void append(AuthSessionAuditEvent event);

    boolean exists(AuthSessionAuditEventType eventType, UUID tokenFamilyId);
}
