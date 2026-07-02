CREATE TABLE medical_documents (
    id UUID PRIMARY KEY,
    visit_id UUID NOT NULL UNIQUE REFERENCES visits(id) ON DELETE CASCADE,
    document_number VARCHAR(100) NOT NULL UNIQUE,
    file_path VARCHAR(500) NOT NULL,
    status VARCHAR(50) NOT NULL DEFAULT 'VALID',
    organization_id UUID NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL
);

CREATE INDEX idx_medical_documents_organization_id ON medical_documents (organization_id);
