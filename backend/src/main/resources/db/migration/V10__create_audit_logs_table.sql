-- STORY-0701 : Creation de la table audit_logs pour la tracabilite des acces et actions
CREATE TABLE audit_logs (
    id UUID PRIMARY KEY,
    actor_user_id UUID,
    actor_organization_id UUID,
    patient_id UUID,
    resource_type VARCHAR(80),
    resource_id UUID,
    action VARCHAR(80) NOT NULL,
    reason TEXT,
    ip_address VARCHAR(80),
    user_agent TEXT,
    status VARCHAR(30) NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL
);

CREATE INDEX idx_audit_logs_patient_id ON audit_logs (patient_id);
CREATE INDEX idx_audit_logs_organization_id ON audit_logs (actor_organization_id);
