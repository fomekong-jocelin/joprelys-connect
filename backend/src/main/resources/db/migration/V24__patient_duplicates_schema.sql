-- V24: Patient duplicates schema (TICKET-1301/1302/1303/1304)

-- Alter patients table to make phone nullable
ALTER TABLE patients ALTER COLUMN phone DROP NOT NULL;

-- Table des doublons candidats détectés automatiquement
CREATE TABLE patient_duplicate_candidates (
    id UUID PRIMARY KEY,
    source_patient_id UUID NOT NULL REFERENCES patients(id) ON DELETE CASCADE,
    target_patient_id UUID NOT NULL REFERENCES patients(id) ON DELETE CASCADE,
    similarity_score DOUBLE PRECISION NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING', -- PENDING, RESOLVED, IGNORED
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT unique_patient_pair UNIQUE (source_patient_id, target_patient_id)
);

-- Table de traçabilité des fusions de dossiers patients
CREATE TABLE patient_merged_history (
    id UUID PRIMARY KEY,
    primary_patient_id UUID NOT NULL REFERENCES patients(id) ON DELETE CASCADE,
    merged_patient_id UUID NOT NULL, -- UUID de l'ancien patient (supprimé/désactivé)
    merged_patient_dpu VARCHAR(50) NOT NULL, -- Sauvegarde de son ancien numéro DPU
    merged_by UUID NOT NULL REFERENCES users(id),
    merged_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);
