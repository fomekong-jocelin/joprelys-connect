-- HOS-BED-001-C : une affectation, son séjour et son lit appartiennent
-- obligatoirement au même établissement.
-- La migration échoue volontairement sur toute incohérence historique.

ALTER TABLE hospitalizations
    ADD CONSTRAINT uq_hospitalizations_id_organization
        UNIQUE (id, organization_id);

ALTER TABLE beds
    ADD CONSTRAINT uq_beds_id_organization
        UNIQUE (id, organization_id);

ALTER TABLE bed_assignments
    ADD CONSTRAINT fk_bed_assignments_hospitalization_organization
        FOREIGN KEY (hospitalization_id, organization_id)
        REFERENCES hospitalizations(id, organization_id)
        ON DELETE RESTRICT;

ALTER TABLE bed_assignments
    ADD CONSTRAINT fk_bed_assignments_bed_organization
        FOREIGN KEY (bed_id, organization_id)
        REFERENCES beds(id, organization_id)
        ON DELETE RESTRICT;
