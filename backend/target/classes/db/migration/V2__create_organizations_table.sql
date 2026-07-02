CREATE TABLE organizations (
    id UUID PRIMARY KEY,
    name VARCHAR(150) NOT NULL,
    email VARCHAR(150) NOT NULL UNIQUE,
    phone VARCHAR(50),
    address VARCHAR(255),
    city VARCHAR(100) NOT NULL,
    logo_path VARCHAR(255),
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL
);

ALTER TABLE users ADD COLUMN organization_id UUID REFERENCES organizations(id);
CREATE INDEX idx_users_organization_id ON users (organization_id);
