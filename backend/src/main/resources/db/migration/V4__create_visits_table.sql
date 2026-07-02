CREATE TABLE visits (
    id UUID PRIMARY KEY,
    organization_id UUID NOT NULL REFERENCES organizations(id),
    patient_id UUID NOT NULL REFERENCES patients(id),
    visit_number VARCHAR(50) NOT NULL UNIQUE,
    reason TEXT NOT NULL,
    orientation VARCHAR(100) NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'EN_COURS',
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL,
    closed_at TIMESTAMP WITH TIME ZONE
);

CREATE INDEX idx_visits_organization_id ON visits (organization_id);
CREATE INDEX idx_visits_patient_id ON visits (patient_id);
CREATE INDEX idx_visits_status ON visits (status);
CREATE INDEX idx_visits_visit_number ON visits (visit_number);
