-- ADR-20261017 D-06 : décision d'orientation distincte de la sortie des urgences.
ALTER TABLE emergencies ADD COLUMN orientation_decided_at TIMESTAMP;
ALTER TABLE emergencies ADD COLUMN orientation_decided_by UUID;
ALTER TABLE emergencies ADD COLUMN stabilized_by UUID;

-- Les urgences déjà clôturées avec une orientation gardent leur date comme date de décision.
UPDATE emergencies SET orientation_decided_at = stabilized_at WHERE stabilized_at IS NOT NULL AND orientation IS NOT NULL;

-- ADR-20261017 D-07 : décès constaté.
ALTER TABLE patients ADD COLUMN deceased_at TIMESTAMP;
ALTER TABLE patients ADD COLUMN death_declared_by UUID;
ALTER TABLE patients ADD COLUMN death_declared_at TIMESTAMP;
ALTER TABLE patients ADD COLUMN death_place VARCHAR(200);
ALTER TABLE patients ADD COLUMN death_circumstances VARCHAR(1000);
