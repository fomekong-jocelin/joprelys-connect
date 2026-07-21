-- HOS-BED-001-A : une seule affectation active par lit.
-- Le marqueur technique rend l'unicité portable entre PostgreSQL 16 et H2.
-- La création de l'index échoue volontairement si des doublons actifs historiques existent.

ALTER TABLE bed_assignments
    ADD COLUMN active_bed_id UUID;

UPDATE bed_assignments
SET active_bed_id = bed_id
WHERE released_at IS NULL;

ALTER TABLE bed_assignments
    ADD CONSTRAINT ck_bed_assignments_active_bed_consistency
        CHECK (
            (released_at IS NULL AND active_bed_id IS NOT NULL AND active_bed_id = bed_id)
            OR
            (released_at IS NOT NULL AND active_bed_id IS NULL)
        );

CREATE UNIQUE INDEX uq_bed_assignments_active_bed
    ON bed_assignments(active_bed_id);
