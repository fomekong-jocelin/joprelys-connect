CREATE TABLE consultations (
    id UUID PRIMARY KEY,
    visit_id UUID NOT NULL UNIQUE REFERENCES visits(id) ON DELETE CASCADE,
    organization_id UUID NOT NULL,
    doctor_id UUID NOT NULL REFERENCES users(id),
    document_number VARCHAR(50) NOT NULL UNIQUE,
    symptoms TEXT NOT NULL,
    clinical_exam TEXT,
    diagnosis TEXT NOT NULL,
    advice TEXT,
    follow_up TEXT,
    status VARCHAR(20) NOT NULL DEFAULT 'BROUILLON',
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL
);

CREATE INDEX idx_consultations_visit_id ON consultations (visit_id);
CREATE INDEX idx_consultations_organization_id ON consultations (organization_id);
CREATE INDEX idx_consultations_doctor_id ON consultations (doctor_id);
