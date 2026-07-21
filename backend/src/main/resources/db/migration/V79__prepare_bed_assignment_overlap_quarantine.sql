-- HOS-BED-001-D : préparer la quarantaine explicite des périodes historiques incohérentes.
-- Cette migration est portable PostgreSQL/H2 et ne modifie aucune période existante.
-- Seules des affectations clôturées peuvent être mises en quarantaine après validation DBA/métier.

ALTER TABLE bed_assignments
    ADD COLUMN integrity_status VARCHAR(20) NOT NULL DEFAULT 'VALID';

ALTER TABLE bed_assignments
    ADD COLUMN quarantined_at TIMESTAMP;

ALTER TABLE bed_assignments
    ADD COLUMN quarantine_reason VARCHAR(500);

ALTER TABLE bed_assignments
    ADD COLUMN quarantined_by VARCHAR(255);

ALTER TABLE bed_assignments
    ADD CONSTRAINT ck_bed_assignments_integrity_quarantine
        CHECK (
            (
                integrity_status = 'VALID'
                AND quarantined_at IS NULL
                AND quarantine_reason IS NULL
                AND quarantined_by IS NULL
            )
            OR
            (
                integrity_status = 'QUARANTINED'
                AND released_at IS NOT NULL
                AND quarantined_at IS NOT NULL
                AND LENGTH(TRIM(quarantine_reason)) > 0
                AND LENGTH(TRIM(quarantined_by)) > 0
            )
        );

CREATE INDEX idx_bed_assignments_integrity_status
    ON bed_assignments(integrity_status);
