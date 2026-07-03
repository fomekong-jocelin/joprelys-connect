-- V20: Création de la table des demandes d'accès externes temporaires (STORY-1301)
CREATE TABLE IF NOT EXISTS external_access_requests (
    id UUID PRIMARY KEY,
    patient_id UUID NOT NULL REFERENCES patients(id) ON DELETE CASCADE,
    requester_user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    requester_organization_id UUID NOT NULL REFERENCES organizations(id) ON DELETE CASCADE,
    reason TEXT NOT NULL,
    requested_duration_hours INT NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'EN_ATTENTE', -- EN_ATTENTE, APPROUVEE, REFUSEE, EXPIREE
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    expires_at TIMESTAMP WITH TIME ZONE,
    version BIGINT NOT NULL DEFAULT 0
);

CREATE INDEX IF NOT EXISTS idx_ext_access_patient ON external_access_requests(patient_id);
CREATE INDEX IF NOT EXISTS idx_ext_access_requester_org ON external_access_requests(requester_organization_id);
CREATE INDEX IF NOT EXISTS idx_ext_access_status ON external_access_requests(status);
