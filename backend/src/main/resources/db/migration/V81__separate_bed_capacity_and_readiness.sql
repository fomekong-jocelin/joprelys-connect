-- HOS-BED-002-C : séparer la capacité ouverte de l'état de préparation du lit.
-- Le champ legacy beds.status est conservé pour compatibilité API et anciens clients.
-- Les contraintes fortes sont installées par V82 uniquement sous PostgreSQL.

ALTER TABLE beds
    ADD COLUMN capacity_status VARCHAR(20) NOT NULL DEFAULT 'OPEN';

ALTER TABLE beds
    ADD COLUMN readiness_status VARCHAR(20) NOT NULL DEFAULT 'READY';

UPDATE beds
SET readiness_status = CASE
    WHEN status = 'CLEANING' THEN 'CLEANING'
    WHEN status = 'MAINTENANCE' THEN 'MAINTENANCE'
    ELSE 'READY'
END;

CREATE INDEX idx_beds_capacity_readiness
    ON beds(organization_id, capacity_status, readiness_status);
