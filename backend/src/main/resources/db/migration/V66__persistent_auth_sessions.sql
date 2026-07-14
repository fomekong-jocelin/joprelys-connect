CREATE TABLE auth_sessions (
    id UUID PRIMARY KEY,
    user_id UUID NOT NULL,
    organization_id UUID,
    token_family_id UUID NOT NULL,
    refresh_token_hash VARCHAR(64) NOT NULL,
    client_type VARCHAR(32) NOT NULL,
    user_agent VARCHAR(160),
    ip_prefix VARCHAR(64),
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    last_used_at TIMESTAMP WITH TIME ZONE NOT NULL,
    absolute_expires_at TIMESTAMP WITH TIME ZONE NOT NULL,
    idle_expires_at TIMESTAMP WITH TIME ZONE NOT NULL,
    revoked_at TIMESTAMP WITH TIME ZONE,
    revocation_reason VARCHAR(32),
    replaced_by_session_id UUID,
    version BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT fk_auth_sessions_user
        FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    CONSTRAINT fk_auth_sessions_organization
        FOREIGN KEY (organization_id) REFERENCES organizations(id),
    CONSTRAINT uq_auth_sessions_refresh_hash UNIQUE (refresh_token_hash),
    CONSTRAINT ck_auth_sessions_absolute_expiry
        CHECK (absolute_expires_at > created_at),
    CONSTRAINT ck_auth_sessions_idle_expiry
        CHECK (idle_expires_at > created_at AND idle_expires_at <= absolute_expires_at),
    CONSTRAINT ck_auth_sessions_revocation
        CHECK (
            (revoked_at IS NULL AND revocation_reason IS NULL)
            OR (revoked_at IS NOT NULL AND revocation_reason IS NOT NULL)
        ),
    CONSTRAINT ck_auth_sessions_replacement
        CHECK (replaced_by_session_id IS NULL OR replaced_by_session_id <> id)
);

ALTER TABLE auth_sessions
    ADD CONSTRAINT fk_auth_sessions_replacement
    FOREIGN KEY (replaced_by_session_id) REFERENCES auth_sessions(id)
    ON DELETE SET NULL;

CREATE INDEX idx_auth_sessions_user_active
    ON auth_sessions (user_id, revoked_at, absolute_expires_at);

CREATE INDEX idx_auth_sessions_family_created
    ON auth_sessions (token_family_id, created_at);

CREATE INDEX idx_auth_sessions_expiry
    ON auth_sessions (absolute_expires_at, idle_expires_at);

CREATE INDEX idx_auth_sessions_organization
    ON auth_sessions (organization_id);
