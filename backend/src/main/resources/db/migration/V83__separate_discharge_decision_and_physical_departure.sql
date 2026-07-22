-- HOS-DIS-001-A : distinguer la décision médicale de sortie du départ physique.
-- Le séjour et l'affectation du lit restent actifs tant que le départ physique n'est pas confirmé.

ALTER TABLE hospitalizations
    ADD COLUMN discharge_decided_at TIMESTAMP;

ALTER TABLE hospitalizations
    ADD COLUMN discharge_decided_by UUID;

ALTER TABLE hospitalizations
    ADD COLUMN discharge_against_medical_advice BOOLEAN NOT NULL DEFAULT FALSE;

ALTER TABLE hospitalizations
    ADD COLUMN physical_departure_at TIMESTAMP;

ALTER TABLE hospitalizations
    ADD COLUMN physical_departure_by UUID;

ALTER TABLE hospitalizations
    ADD COLUMN physical_departure_note VARCHAR(500);

-- Les sorties historiques étaient atomiques : la décision et le départ physique
-- sont donc rétrodéduits de discharged_at sans modifier leur statut final.
UPDATE hospitalizations
SET discharge_decided_at = discharged_at,
    physical_departure_at = discharged_at,
    discharge_against_medical_advice = CASE
        WHEN status = 'SORTI_CONTRE_AVIS' THEN TRUE
        ELSE FALSE
    END
WHERE discharged_at IS NOT NULL;

CREATE INDEX idx_hospitalizations_discharge_workflow
    ON hospitalizations(organization_id, discharge_decided_at, physical_departure_at);
