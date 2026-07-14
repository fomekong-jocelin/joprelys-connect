ALTER TABLE auth_sessions ADD COLUMN revoked_by_user_id UUID;
ALTER TABLE auth_sessions ADD COLUMN revocation_source VARCHAR(16);

UPDATE auth_sessions
SET revocation_source = 'SYSTEM'
WHERE revoked_at IS NOT NULL
  AND revocation_source IS NULL;

ALTER TABLE auth_sessions DROP CONSTRAINT ck_auth_sessions_revocation;
ALTER TABLE auth_sessions
    ADD CONSTRAINT ck_auth_sessions_revocation
    CHECK (
        (revoked_at IS NULL
            AND revocation_reason IS NULL
            AND revocation_source IS NULL
            AND revoked_by_user_id IS NULL)
        OR
        (revoked_at IS NOT NULL
            AND revocation_reason IS NOT NULL
            AND revocation_source IS NOT NULL)
    );

ALTER TABLE auth_sessions
    ADD CONSTRAINT fk_auth_sessions_revoked_by_user
    FOREIGN KEY (revoked_by_user_id) REFERENCES users(id) ON DELETE SET NULL;

ALTER TABLE auth_sessions
    ADD CONSTRAINT ck_auth_sessions_revocation_source
    CHECK (
        revocation_source IS NULL
        OR revocation_source = 'SELF'
        OR revocation_source = 'ADMIN'
        OR revocation_source = 'SYSTEM'
    );

CREATE INDEX idx_auth_sessions_revoked_by
    ON auth_sessions (revoked_by_user_id, revoked_at);

CREATE TABLE revoked_access_tokens (
    token_id VARCHAR(64) PRIMARY KEY,
    user_id UUID,
    organization_id UUID,
    revoked_at TIMESTAMP WITH TIME ZONE NOT NULL,
    expires_at TIMESTAMP WITH TIME ZONE NOT NULL,
    reason VARCHAR(32) NOT NULL,
    revoked_by_user_id UUID,
    CONSTRAINT fk_revoked_access_tokens_user
        FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE SET NULL,
    CONSTRAINT fk_revoked_access_tokens_organization
        FOREIGN KEY (organization_id) REFERENCES organizations(id) ON DELETE SET NULL,
    CONSTRAINT fk_revoked_access_tokens_actor
        FOREIGN KEY (revoked_by_user_id) REFERENCES users(id) ON DELETE SET NULL,
    CONSTRAINT ck_revoked_access_tokens_expiry
        CHECK (expires_at > revoked_at)
);

CREATE INDEX idx_revoked_access_tokens_expiry
    ON revoked_access_tokens (expires_at);

CREATE TABLE auth_session_audit_events (
    id UUID PRIMARY KEY,
    organization_id UUID,
    actor_user_id UUID,
    target_user_id UUID NOT NULL,
    session_id UUID,
    token_family_id UUID,
    event_type VARCHAR(48) NOT NULL,
    reason VARCHAR(64),
    occurred_at TIMESTAMP WITH TIME ZONE NOT NULL,
    CONSTRAINT ck_auth_session_audit_event_type
        CHECK (
            event_type = 'SESSION_CREATED'
            OR event_type = 'SESSION_ROTATED'
            OR event_type = 'SESSION_REVOKED'
            OR event_type = 'LOGOUT_ALL'
            OR event_type = 'REFRESH_REPLAY_DETECTED'
            OR event_type = 'LEGACY_ACCESS_TOKEN_REVOKED'
        )
);

CREATE INDEX idx_auth_session_audit_org_created
    ON auth_session_audit_events (organization_id, occurred_at);
CREATE INDEX idx_auth_session_audit_target_created
    ON auth_session_audit_events (target_user_id, occurred_at);
CREATE INDEX idx_auth_session_audit_family_created
    ON auth_session_audit_events (token_family_id, occurred_at);
