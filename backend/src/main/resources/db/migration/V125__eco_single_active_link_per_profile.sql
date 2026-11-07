-- ECO-20261107 S8 (revue sécurité) : une seule liaison active par profil d'application, garantie par la base.
-- Sans cet index, deux créations simultanées pouvaient relier un même profil à deux DPU (lecture puis écriture).
-- Marqueur technique portable PostgreSQL / H2, sur le modèle de V76 : renseigné tant que la liaison est active.

ALTER TABLE eco_patient_links
    ADD COLUMN active_external_profile_id UUID;

UPDATE eco_patient_links
SET active_external_profile_id = external_profile_id
WHERE status = 'ACTIVE';

ALTER TABLE eco_patient_links
    ADD CONSTRAINT ck_eco_patient_links_active_profile
        CHECK (
            (status = 'ACTIVE' AND active_external_profile_id IS NOT NULL
                AND active_external_profile_id = external_profile_id)
            OR
            (status = 'REVOKED' AND active_external_profile_id IS NULL)
        );

CREATE UNIQUE INDEX uq_eco_patient_links_active_profile
    ON eco_patient_links (application, active_external_profile_id);
