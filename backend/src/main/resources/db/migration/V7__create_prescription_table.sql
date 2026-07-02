CREATE TABLE prescriptions (
    id UUID PRIMARY KEY,
    consultation_id UUID NOT NULL UNIQUE REFERENCES consultations(id) ON DELETE CASCADE,
    organization_id UUID NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL
);

CREATE TABLE prescription_items (
    id UUID PRIMARY KEY,
    prescription_id UUID NOT NULL REFERENCES prescriptions(id) ON DELETE CASCADE,
    drug_name VARCHAR(200) NOT NULL,
    dosage VARCHAR(200) NOT NULL,
    posology VARCHAR(500),
    duration VARCHAR(100),
    quantity VARCHAR(100),
    instructions TEXT,
    sort_order INT NOT NULL DEFAULT 0
);

CREATE INDEX idx_prescriptions_consultation_id ON prescriptions (consultation_id);
CREATE INDEX idx_prescriptions_organization_id ON prescriptions (organization_id);
CREATE INDEX idx_prescription_items_prescription_id ON prescription_items (prescription_id);
