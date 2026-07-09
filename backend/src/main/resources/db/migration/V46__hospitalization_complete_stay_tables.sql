-- Migration V46: Tables de séjour hospitalier complet, sortie contre avis et consentements opératoires (STORY-2102)

-- Augmenter la taille de la colonne status pour supporter des statuts plus longs comme SORTI_CONTRE_AVIS
ALTER TABLE hospitalizations ALTER COLUMN status TYPE VARCHAR(50);

-- Table des consentements opératoires (anesthésie / chirurgie)
CREATE TABLE IF NOT EXISTS surgical_consents (
    id UUID PRIMARY KEY,
    hospitalization_id UUID NOT NULL REFERENCES hospitalizations(id) ON DELETE CASCADE,
    organization_id UUID NOT NULL,
    version BIGINT NOT NULL DEFAULT 0,
    consent_type VARCHAR(50) NOT NULL, -- ANESTHESIA, SURGERY
    patient_signature_present BOOLEAN NOT NULL DEFAULT FALSE,
    witness_name VARCHAR(100),
    document_id UUID REFERENCES medical_documents(id) ON DELETE SET NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL
);

CREATE INDEX IF NOT EXISTS idx_surgical_consents_hosp_id ON surgical_consents(hospitalization_id);
CREATE INDEX IF NOT EXISTS idx_surgical_consents_org_id ON surgical_consents(organization_id);
