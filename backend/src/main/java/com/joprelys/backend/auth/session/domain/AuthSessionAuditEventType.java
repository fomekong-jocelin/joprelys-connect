package com.joprelys.backend.auth.session.domain;

public enum AuthSessionAuditEventType {
    SESSION_CREATED,
    SESSION_ROTATED,
    SESSION_REVOKED,
    LOGOUT_ALL,
    REFRESH_REPLAY_DETECTED,
    LEGACY_ACCESS_TOKEN_REVOKED
}
