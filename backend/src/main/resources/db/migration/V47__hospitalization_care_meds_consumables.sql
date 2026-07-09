-- Migration V47: Tables pour les soins journaliers, administrations de médicaments et consommables (STORY-2103)

-- Table des soins journaliers
CREATE TABLE IF NOT EXISTS hospitalization_daily_cares (
    id UUID PRIMARY KEY,
    hospitalization_id UUID NOT NULL REFERENCES hospitalizations(id) ON DELETE CASCADE,
    organization_id UUID NOT NULL,
    version BIGINT NOT NULL DEFAULT 0,
    care_type VARCHAR(100) NOT NULL, -- PANSEMENT, INJECTION, PERFUSION, TOILETTE, etc.
    description VARCHAR(500),
    billable BOOLEAN NOT NULL DEFAULT FALSE,
    price DOUBLE PRECISION,
    performed_by VARCHAR(100) NOT NULL,
    performed_at TIMESTAMP WITH TIME ZONE NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL
);

CREATE INDEX IF NOT EXISTS idx_daily_cares_hosp_id ON hospitalization_daily_cares(hospitalization_id);
CREATE INDEX IF NOT EXISTS idx_daily_cares_org_id ON hospitalization_daily_cares(organization_id);

-- Table de l'administration horodatée des médicaments
CREATE TABLE IF NOT EXISTS medication_administrations (
    id UUID PRIMARY KEY,
    hospitalization_id UUID NOT NULL REFERENCES hospitalizations(id) ON DELETE CASCADE,
    prescription_item_id UUID, -- Optionnel, peut être lié à un item de prescription
    organization_id UUID NOT NULL,
    version BIGINT NOT NULL DEFAULT 0,
    medication_name VARCHAR(200) NOT NULL,
    dose VARCHAR(100) NOT NULL,
    administered_by VARCHAR(100) NOT NULL,
    administered_at TIMESTAMP WITH TIME ZONE NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL
);

CREATE INDEX IF NOT EXISTS idx_med_admin_hosp_id ON medication_administrations(hospitalization_id);
CREATE INDEX IF NOT EXISTS idx_med_admin_org_id ON medication_administrations(organization_id);

-- Table des consommables et médicaments consommés par le patient
CREATE TABLE IF NOT EXISTS patient_consumptions (
    id UUID PRIMARY KEY,
    hospitalization_id UUID NOT NULL REFERENCES hospitalizations(id) ON DELETE CASCADE,
    organization_id UUID NOT NULL,
    version BIGINT NOT NULL DEFAULT 0,
    item_name VARCHAR(200) NOT NULL, -- Pansement, Seringue, Paracétamol, etc.
    quantity INTEGER NOT NULL DEFAULT 1,
    unit_price DOUBLE PRECISION NOT NULL DEFAULT 0.0,
    consumed_by VARCHAR(100),
    consumed_at TIMESTAMP WITH TIME ZONE NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL
);

CREATE INDEX IF NOT EXISTS idx_patient_consumptions_hosp_id ON patient_consumptions(hospitalization_id);
CREATE INDEX IF NOT EXISTS idx_patient_consumptions_org_id ON patient_consumptions(organization_id);
