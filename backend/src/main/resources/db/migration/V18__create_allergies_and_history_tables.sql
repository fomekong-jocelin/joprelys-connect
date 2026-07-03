-- V18: Création des tables d'allergies et antécédents médicaux (STORY-1201)
CREATE TABLE IF NOT EXISTS patient_allergies (
    id UUID PRIMARY KEY,
    patient_id UUID NOT NULL REFERENCES patients(id) ON DELETE CASCADE,
    organization_id UUID NOT NULL,
    version BIGINT NOT NULL DEFAULT 0,
    substance VARCHAR(255) NOT NULL,
    severity VARCHAR(50) NOT NULL, -- LOW, MEDIUM, HIGH, CRITICAL
    reaction VARCHAR(255),
    status VARCHAR(50) NOT NULL DEFAULT 'ACTIVE', -- ACTIVE, INACTIVE
    discovered_at DATE,
    comment TEXT,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL
);

CREATE TABLE IF NOT EXISTS patient_medical_history (
    id UUID PRIMARY KEY,
    patient_id UUID NOT NULL REFERENCES patients(id) ON DELETE CASCADE,
    organization_id UUID NOT NULL,
    version BIGINT NOT NULL DEFAULT 0,
    category VARCHAR(50) NOT NULL, -- MEDICAL, SURGICAL, FAMILY, OBSTETRICAL, OTHER
    description VARCHAR(255) NOT NULL,
    onset_date DATE,
    is_ongoing BOOLEAN NOT NULL DEFAULT FALSE,
    comment TEXT,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL
);

CREATE INDEX IF NOT EXISTS idx_patient_allergies_patient_id ON patient_allergies(patient_id);
CREATE INDEX IF NOT EXISTS idx_patient_allergies_org_id ON patient_allergies(organization_id);
CREATE INDEX IF NOT EXISTS idx_patient_medical_history_patient_id ON patient_medical_history(patient_id);
CREATE INDEX IF NOT EXISTS idx_patient_medical_history_org_id ON patient_medical_history(organization_id);
