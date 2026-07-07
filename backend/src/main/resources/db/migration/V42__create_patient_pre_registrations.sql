-- Flyway Migration V42: Create patient_pre_registrations table for staging area
CREATE TABLE patient_pre_registrations (
    id UUID PRIMARY KEY,
    organization_id UUID NOT NULL,
    first_name VARCHAR(100) NOT NULL,
    last_name VARCHAR(100) NOT NULL,
    gender VARCHAR(20) NOT NULL,
    birth_date DATE NOT NULL,
    blood_group VARCHAR(10),
    phone VARCHAR(50),
    email VARCHAR(255),
    address TEXT,
    emergency_contact_name VARCHAR(150),
    emergency_contact_phone VARCHAR(50),
    emergency_contact_relation VARCHAR(50),
    status VARCHAR(30) NOT NULL DEFAULT 'AWAITING_VALIDATION',
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    validated_at TIMESTAMP,
    validated_by UUID
);

CREATE INDEX idx_pre_reg_org_status ON patient_pre_registrations(organization_id, status);
CREATE INDEX idx_pre_reg_created_at ON patient_pre_registrations(created_at);
