-- HOS-BED-001-B : rattachement obligatoire au séjour, une présence active par séjour
-- et chronologie valide des affectations.
-- La migration échoue volontairement sur les orphelins, doublons actifs ou périodes inversées.

ALTER TABLE bed_assignments
    ADD COLUMN active_hospitalization_id UUID;

UPDATE bed_assignments
SET active_hospitalization_id = hospitalization_id
WHERE released_at IS NULL;

ALTER TABLE bed_assignments
    ADD CONSTRAINT fk_bed_assignments_hospitalization
        FOREIGN KEY (hospitalization_id) REFERENCES hospitalizations(id) ON DELETE RESTRICT;

ALTER TABLE bed_assignments
    ADD CONSTRAINT ck_bed_assignments_active_hospitalization_consistency
        CHECK (
            (released_at IS NULL
                AND active_hospitalization_id IS NOT NULL
                AND active_hospitalization_id = hospitalization_id)
            OR
            (released_at IS NOT NULL AND active_hospitalization_id IS NULL)
        );

ALTER TABLE bed_assignments
    ADD CONSTRAINT ck_bed_assignments_assignment_period
        CHECK (released_at IS NULL OR released_at >= assigned_at);

CREATE UNIQUE INDEX uq_bed_assignments_active_hospitalization
    ON bed_assignments(active_hospitalization_id);
