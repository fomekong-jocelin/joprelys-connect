-- HOS-02 / #73 : le type métier du service devient obligatoire.
-- Aucun défaut ni déduction par le nom : les données ambiguës doivent être qualifiées avant déploiement.

ALTER TABLE wards
    ADD COLUMN service_type VARCHAR(40);

-- Une relation existante vers une chambre prouve que le service historique est spatial.
UPDATE wards w
SET service_type = 'HOSPITALIZATION'
WHERE EXISTS (
    SELECT 1
    FROM rooms r
    WHERE r.ward_id = w.id
);

-- Échec volontaire si des services historiques sans chambre n'ont pas été qualifiés explicitement.
ALTER TABLE wards
    ALTER COLUMN service_type SET NOT NULL;

ALTER TABLE wards
    ADD CONSTRAINT chk_wards_service_type
    CHECK (service_type IN (
        'HOSPITALIZATION',
        'EMERGENCY',
        'OUTPATIENT',
        'MEDICO_TECHNICAL',
        'PHARMACY',
        'ADMINISTRATIVE'
    ));
