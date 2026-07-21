-- HOS-BED-002-C : séparer la capacité ouverte de l'état de préparation du lit.
-- Le champ legacy beds.status est conservé pour compatibilité API et anciens clients.

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

ALTER TABLE beds
    ADD CONSTRAINT ck_beds_capacity_status
        CHECK (capacity_status IN ('OPEN', 'CLOSED'));

ALTER TABLE beds
    ADD CONSTRAINT ck_beds_readiness_status
        CHECK (readiness_status IN ('READY', 'CLEANING', 'MAINTENANCE'));

ALTER TABLE beds
    ADD CONSTRAINT ck_beds_legacy_status_projection
        CHECK (
            (status = 'FREE' AND capacity_status = 'OPEN' AND readiness_status = 'READY')
            OR (status = 'OCCUPIED' AND capacity_status = 'OPEN' AND readiness_status = 'READY')
            OR (status = 'CLEANING' AND readiness_status = 'CLEANING')
            OR (
                status = 'MAINTENANCE'
                AND (readiness_status = 'MAINTENANCE' OR capacity_status = 'CLOSED')
            )
        );

CREATE INDEX idx_beds_capacity_readiness
    ON beds(organization_id, capacity_status, readiness_status);
