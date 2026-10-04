-- Étape de prise en charge d'une visite active, indépendante du statut administratif (EN_COURS/TERMINEE/ANNULEE).
ALTER TABLE visits ADD COLUMN care_stage VARCHAR(30) DEFAULT 'ATTENTE_CONSTANTES' NOT NULL;
ALTER TABLE visits ADD COLUMN consulting_practitioner_id UUID;
ALTER TABLE visits ADD COLUMN consulting_practitioner_name VARCHAR(255);
ALTER TABLE visits ADD COLUMN consultation_started_at TIMESTAMP WITH TIME ZONE;

UPDATE visits SET care_stage = 'PRET_MEDECIN'
WHERE EXISTS (SELECT 1 FROM vitals v WHERE v.visit_id = visits.id);

UPDATE visits SET
    care_stage = 'EN_CONSULTATION',
    consulting_practitioner_id = (SELECT c.doctor_id FROM consultations c WHERE c.visit_id = visits.id),
    consultation_started_at = (SELECT c.created_at FROM consultations c WHERE c.visit_id = visits.id)
WHERE EXISTS (SELECT 1 FROM consultations c WHERE c.visit_id = visits.id);

UPDATE visits SET consulting_practitioner_name = (
    SELECT u.display_name FROM users u WHERE u.id = visits.consulting_practitioner_id)
WHERE consulting_practitioner_id IS NOT NULL;

-- Historique horodaté et signé des mesures de constantes (la table vitals garde la dernière mesure).
CREATE TABLE visit_vital_measurements (
    id UUID PRIMARY KEY,
    organization_id UUID,
    visit_id UUID NOT NULL REFERENCES visits(id) ON DELETE CASCADE,
    recorded_by UUID,
    recorded_by_name VARCHAR(255),
    recorded_at TIMESTAMP WITH TIME ZONE NOT NULL,
    temperature DECIMAL(3,1),
    weight DECIMAL(4,1),
    height INTEGER,
    pulse INTEGER,
    systolic INTEGER,
    diastolic INTEGER,
    spo2 INTEGER,
    glycemia DECIMAL(3,2),
    respiratory_rate INTEGER,
    pain_scale INTEGER,
    bmi DECIMAL(4,2)
);

CREATE INDEX idx_vital_measurements_visit ON visit_vital_measurements (visit_id, recorded_at);

INSERT INTO visit_vital_measurements (
    id, organization_id, visit_id, recorded_by, recorded_by_name, recorded_at,
    temperature, weight, height, pulse, systolic, diastolic, spo2, glycemia, respiratory_rate, pain_scale, bmi)
SELECT v.id, vi.organization_id, v.visit_id, NULL, NULL, v.updated_at,
    v.temperature, v.weight, v.height, v.pulse, v.systolic, v.diastolic, v.spo2, v.glycemia, v.respiratory_rate, v.pain_scale, v.bmi
FROM vitals v JOIN visits vi ON vi.id = v.visit_id;
