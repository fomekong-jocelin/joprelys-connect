-- V17: Gestion reelle des stocks de medicaments (STORY-1103)
CREATE TABLE IF NOT EXISTS drug_stocks (
    id UUID PRIMARY KEY,
    organization_id UUID NOT NULL,
    version BIGINT NOT NULL DEFAULT 0,
    drug_name VARCHAR(255) NOT NULL,
    generic_name VARCHAR(255),
    unit VARCHAR(50) NOT NULL DEFAULT 'comprime',
    quantity_available INTEGER NOT NULL DEFAULT 0 CHECK (quantity_available >= 0),
    minimum_threshold INTEGER NOT NULL DEFAULT 10,
    batch_number VARCHAR(100),
    expiry_date DATE,
    supplier VARCHAR(255),
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_drug_stocks_organization_id ON drug_stocks(organization_id);
CREATE INDEX IF NOT EXISTS idx_drug_stocks_drug_name ON drug_stocks(organization_id, drug_name);
