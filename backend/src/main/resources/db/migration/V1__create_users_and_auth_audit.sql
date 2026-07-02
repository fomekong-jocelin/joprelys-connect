CREATE TABLE users (
    id UUID PRIMARY KEY,
    email VARCHAR(320) NOT NULL UNIQUE,
    display_name VARCHAR(160) NOT NULL,
    role VARCHAR(64) NOT NULL,
    password_hash VARCHAR(120) NOT NULL,
    enabled BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL
);

CREATE INDEX idx_users_email ON users (email);

CREATE TABLE auth_audit_events (
    id UUID PRIMARY KEY,
    occurred_at TIMESTAMP WITH TIME ZONE NOT NULL,
    email VARCHAR(320) NOT NULL,
    ip_address VARCHAR(64) NOT NULL,
    success BOOLEAN NOT NULL,
    failure_reason VARCHAR(80)
);

CREATE INDEX idx_auth_audit_events_email_occurred_at
    ON auth_audit_events (email, occurred_at DESC);
