-- Un seul diagnostic actif par consultation.
-- Les anciennes valeurs sont conservées dans une archive technique avant
-- suppression des colonnes redondantes du modèle métier.

CREATE TABLE consultation_diagnosis_migration_archive (
    consultation_id UUID PRIMARY KEY REFERENCES consultations(id) ON DELETE CASCADE,
    organization_id UUID NOT NULL REFERENCES organizations(id),
    previous_diagnosis TEXT NOT NULL,
    previous_suspected_diagnosis TEXT,
    previous_final_diagnosis TEXT,
    migrated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_consultation_diagnosis_archive_organization
    ON consultation_diagnosis_migration_archive (organization_id);

INSERT INTO consultation_diagnosis_migration_archive (
    consultation_id,
    organization_id,
    previous_diagnosis,
    previous_suspected_diagnosis,
    previous_final_diagnosis
)
SELECT
    id,
    organization_id,
    diagnosis,
    suspected_diagnosis,
    final_diagnosis
FROM consultations
WHERE NULLIF(TRIM(suspected_diagnosis), '') IS NOT NULL
   OR NULLIF(TRIM(final_diagnosis), '') IS NOT NULL;

UPDATE consultations
SET diagnosis = COALESCE(
    NULLIF(TRIM(final_diagnosis), ''),
    NULLIF(TRIM(diagnosis), ''),
    NULLIF(TRIM(suspected_diagnosis), '')
)
WHERE NULLIF(TRIM(final_diagnosis), '') IS NOT NULL
   OR NULLIF(TRIM(suspected_diagnosis), '') IS NOT NULL;

ALTER TABLE consultations DROP COLUMN IF EXISTS suspected_diagnosis;
ALTER TABLE consultations DROP COLUMN IF EXISTS final_diagnosis;
