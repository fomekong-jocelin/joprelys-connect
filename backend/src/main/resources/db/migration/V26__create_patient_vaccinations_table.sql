-- V26: Create patient vaccinations table (Module 4 / DPU compliance)

CREATE TABLE patient_vaccinations (
    id UUID PRIMARY KEY,
    organization_id UUID NOT NULL REFERENCES organizations(id),
    version BIGINT NOT NULL DEFAULT 0,
    patient_id UUID NOT NULL REFERENCES patients(id) ON DELETE CASCADE,
    vaccine_name VARCHAR(255) NOT NULL,
    batch_number VARCHAR(50),
    administered_at DATE NOT NULL,
    administered_by VARCHAR(255),
    notes TEXT,
    next_dose_at DATE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL
);

CREATE INDEX idx_patient_vaccinations_patient_id ON patient_vaccinations (patient_id);
CREATE INDEX idx_patient_vaccinations_organization_id ON patient_vaccinations (organization_id);
