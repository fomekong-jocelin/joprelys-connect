ALTER TABLE organizations ADD COLUMN type VARCHAR(50) DEFAULT 'CLINIC';
ALTER TABLE organizations ADD COLUMN country VARCHAR(100) DEFAULT 'Cameroun';
ALTER TABLE organizations ADD COLUMN responsible_name VARCHAR(150) DEFAULT 'Responsable';
ALTER TABLE organizations ADD COLUMN api_enabled BOOLEAN DEFAULT TRUE;

CREATE TABLE organization_api_keys (
    id UUID PRIMARY KEY,
    organization_id UUID NOT NULL REFERENCES organizations(id) ON DELETE CASCADE,
    hashed_key VARCHAR(64) NOT NULL UNIQUE,
    prefix VARCHAR(20) NOT NULL,
    name VARCHAR(100) NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    revoked_at TIMESTAMP WITH TIME ZONE
);

CREATE INDEX idx_org_api_keys_org_id ON organization_api_keys (organization_id);
CREATE INDEX idx_org_api_keys_hash ON organization_api_keys (hashed_key);
