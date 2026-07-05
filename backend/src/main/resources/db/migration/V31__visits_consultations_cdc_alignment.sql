-- Alignement Module 5 - Visites et consultations conformes CDC
-- Ticket STORY-1902

-- Ajout des champs manquants sur les visites (un ALTER par colonne pour compatibilite H2)
ALTER TABLE visits ADD COLUMN IF NOT EXISTS service_name VARCHAR(100);
ALTER TABLE visits ADD COLUMN IF NOT EXISTS main_practitioner_id UUID;
ALTER TABLE visits ADD COLUMN IF NOT EXISTS arrival_at TIMESTAMP;

CREATE INDEX IF NOT EXISTS idx_visits_service_name ON visits (service_name);
CREATE INDEX IF NOT EXISTS idx_visits_main_practitioner_id ON visits (main_practitioner_id);
CREATE INDEX IF NOT EXISTS idx_visits_arrival_at ON visits (arrival_at);

-- Ajout de l'echelle de douleur dans les constantes vitales
ALTER TABLE vitals ADD COLUMN IF NOT EXISTS pain_scale INTEGER;

-- Ajout des champs de diagnostic detailles sur les consultations
ALTER TABLE consultations ADD COLUMN IF NOT EXISTS suspected_diagnosis TEXT;
ALTER TABLE consultations ADD COLUMN IF NOT EXISTS final_diagnosis TEXT;
ALTER TABLE consultations ADD COLUMN IF NOT EXISTS conclusion TEXT;

-- Table de tracabilite des corrections sur visites terminees (FR-VISIT-005)
CREATE TABLE IF NOT EXISTS visit_corrections (
    id UUID PRIMARY KEY,
    visit_id UUID NOT NULL,
    corrected_by_user_id UUID,
    correction_reason TEXT NOT NULL,
    previous_values TEXT NOT NULL,
    created_at TIMESTAMP NOT NULL
);

CREATE INDEX IF NOT EXISTS idx_visit_corrections_visit_id ON visit_corrections (visit_id);
CREATE INDEX IF NOT EXISTS idx_visit_corrections_created_at ON visit_corrections (created_at);
