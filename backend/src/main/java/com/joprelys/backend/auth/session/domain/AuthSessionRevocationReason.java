package com.joprelys.backend.auth.session.domain;

public enum AuthSessionRevocationReason {
    ROTATED,
    EXPIRED,
    LOGOUT,
    LOGOUT_ALL,
    REPLAY_DETECTED,
    USER_DISABLED,
    ORGANIZATION_INACTIVE,
    ADMIN_REVOKED,
    MFA_POLICY_CHANGE
}
