-- V19: Création des tables d'hospitalisation et notes d'évolution (STORY-1202)
CREATE TABLE IF NOT EXISTS hospitalizations (
    id UUID PRIMARY KEY,
    patient_id UUID NOT NULL REFERENCES patients(id) ON DELETE CASCADE,
    organization_id UUID NOT NULL,
    version BIGINT NOT NULL DEFAULT 0,
    service_name VARCHAR(100) NOT NULL,
    room_number VARCHAR(50) NOT NULL,
    bed_number VARCHAR(50) NOT NULL,
    admission_reason TEXT NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'EN_COURS', -- EN_COURS, SORTI
    admitted_at TIMESTAMP WITH TIME ZONE NOT NULL,
    discharged_at TIMESTAMP WITH TIME ZONE,
    discharge_diagnosis TEXT,
    discharge_instructions TEXT,
    pdf_file_path VARCHAR(255),
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL
);

CREATE TABLE IF NOT EXISTS hospitalization_notes (
    id UUID PRIMARY KEY,
    hospitalization_id UUID NOT NULL REFERENCES hospitalizations(id) ON DELETE CASCADE,
    organization_id UUID NOT NULL,
    author_name VARCHAR(255) NOT NULL,
    note_content TEXT NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL
);

CREATE INDEX IF NOT EXISTS idx_hospitalizations_patient_id ON hospitalizations(patient_id);
CREATE INDEX IF NOT EXISTS idx_hospitalization_notes_hosp_id ON hospitalization_notes(hospitalization_id);

