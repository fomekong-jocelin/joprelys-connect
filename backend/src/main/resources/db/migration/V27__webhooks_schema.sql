CREATE TABLE webhooks (
    id UUID PRIMARY KEY,
    organization_id UUID NOT NULL,
    url VARCHAR(500) NOT NULL,
    secret VARCHAR(200) NOT NULL,
    event_types VARCHAR(300) NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    last_delivered_at TIMESTAMP,
    failure_count INTEGER DEFAULT 0
);

CREATE INDEX idx_webhooks_org_status ON webhooks(organization_id, status);
CREATE INDEX idx_webhooks_status ON webhooks(status);
