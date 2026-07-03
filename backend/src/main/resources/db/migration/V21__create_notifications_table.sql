-- V21: Création de la table des notifications (STORY-1501)
CREATE TABLE IF NOT EXISTS notifications (
    id UUID PRIMARY KEY,
    patient_id UUID NOT NULL REFERENCES patients(id) ON DELETE CASCADE,
    title VARCHAR(150) NOT NULL,
    message TEXT NOT NULL,
    type VARCHAR(30) NOT NULL DEFAULT 'INFO', -- INFO, SECURITY, EMERGENCY
    status VARCHAR(20) NOT NULL DEFAULT 'NON_LU', -- LU, NON_LU
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    version BIGINT NOT NULL DEFAULT 0
);

CREATE INDEX IF NOT EXISTS idx_notifications_patient ON notifications(patient_id);
CREATE INDEX IF NOT EXISTS idx_notifications_status ON notifications(status);
