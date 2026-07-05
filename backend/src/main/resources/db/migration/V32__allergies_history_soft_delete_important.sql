-- Alignement Module 6 — Allergies et antécédents conformes CDC
-- Ticket STORY-1903

-- Ajout des colonnes sur patient_medical_history
ALTER TABLE patient_medical_history ADD COLUMN IF NOT EXISTS important BOOLEAN DEFAULT FALSE;
ALTER TABLE patient_medical_history ADD COLUMN IF NOT EXISTS deleted_at TIMESTAMP;
ALTER TABLE patient_medical_history ADD COLUMN IF NOT EXISTS deleted_by UUID;

-- Ajout des colonnes sur patient_allergies
ALTER TABLE patient_allergies ADD COLUMN IF NOT EXISTS deleted_at TIMESTAMP;
ALTER TABLE patient_allergies ADD COLUMN IF NOT EXISTS deleted_by UUID;
