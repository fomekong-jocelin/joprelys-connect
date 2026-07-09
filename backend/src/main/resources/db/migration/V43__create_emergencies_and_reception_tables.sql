-- STORY-1601 : Conception du modèle de données (Accueil & Urgences)
-- Création des tables reception_logs, emergencies, resuscitation_logs

CREATE TABLE IF NOT EXISTS reception_logs (
    id UUID PRIMARY KEY,
    organization_id UUID NOT NULL,
    log_type VARCHAR(50) NOT NULL,
    first_name VARCHAR(100) NOT NULL,
    last_name VARCHAR(100) NOT NULL,
    id_document_type VARCHAR(50),
    id_document_number VARCHAR(100),
    target_patient_id UUID,
    target_staff_id UUID,
    reason TEXT,
    arrival_at TIMESTAMP NOT NULL,
    departure_at TIMESTAMP,
    created_by_user_id UUID,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL
);

CREATE INDEX IF NOT EXISTS idx_reception_logs_organization_id ON reception_logs (organization_id);
CREATE INDEX IF NOT EXISTS idx_reception_logs_log_type ON reception_logs (log_type);
CREATE INDEX IF NOT EXISTS idx_reception_logs_target_patient_id ON reception_logs (target_patient_id);

CREATE TABLE IF NOT EXISTS emergencies (
    id UUID PRIMARY KEY,
    organization_id UUID NOT NULL,
    patient_id UUID NOT NULL,
    visit_id UUID,
    arrival_mode VARCHAR(50) NOT NULL,
    triage_level VARCHAR(20) NOT NULL,
    hemodynamic_status VARCHAR(50) NOT NULL,
    chief_complaint TEXT NOT NULL,
    initial_bp_systolic INTEGER,
    initial_bp_diastolic INTEGER,
    initial_hr INTEGER,
    initial_temp DECIMAL(4,2),
    stabilized_at TIMESTAMP,
    orientation VARCHAR(50),
    created_by_user_id UUID,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL
);

CREATE INDEX IF NOT EXISTS idx_emergencies_organization_id ON emergencies (organization_id);
CREATE INDEX IF NOT EXISTS idx_emergencies_patient_id ON emergencies (patient_id);
CREATE INDEX IF NOT EXISTS idx_emergencies_visit_id ON emergencies (visit_id);

CREATE TABLE IF NOT EXISTS resuscitation_logs (
    id UUID PRIMARY KEY,
    emergency_id UUID NOT NULL,
    action_type VARCHAR(50) NOT NULL,
    description VARCHAR(255) NOT NULL,
    quantity DECIMAL(10,2),
    unit VARCHAR(20),
    administered_at TIMESTAMP NOT NULL,
    administered_by_user_id UUID,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL
);

CREATE INDEX IF NOT EXISTS idx_resuscitation_logs_emergency_id ON resuscitation_logs (emergency_id);
