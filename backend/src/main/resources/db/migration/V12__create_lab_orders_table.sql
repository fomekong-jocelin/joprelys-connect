CREATE TABLE lab_orders (
    id UUID PRIMARY KEY,
    exam_request_number VARCHAR(50) UNIQUE NOT NULL,
    patient_id UUID NOT NULL REFERENCES patients(id) ON DELETE CASCADE,
    visit_id UUID REFERENCES visits(id) ON DELETE SET NULL,
    requester_practitioner_id UUID NOT NULL REFERENCES users(id),
    organization_id UUID NOT NULL REFERENCES organizations(id),
    target_organization_id UUID REFERENCES organizations(id),
    exam_type VARCHAR(30) NOT NULL,
    exams TEXT NOT NULL,
    reason VARCHAR(255),
    priority VARCHAR(20) NOT NULL DEFAULT 'NORMALE',
    status VARCHAR(30) NOT NULL DEFAULT 'REQUESTED',
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);
