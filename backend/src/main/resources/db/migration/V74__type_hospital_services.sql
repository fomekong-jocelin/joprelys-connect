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

-- LIKE sans joker conserve une égalité stricte tout en évitant le cache IN de H2,
-- qui devient invalide lorsque plusieurs contextes de test ferment et rouvrent la base embarquée.
-- PostgreSQL applique la même contrainte exacte en production.
ALTER TABLE wards
    ADD CONSTRAINT chk_wards_service_type
    CHECK (
        service_type LIKE 'HOSPITALIZATION'
        OR service_type LIKE 'EMERGENCY'
        OR service_type LIKE 'OUTPATIENT'
        OR service_type LIKE 'MEDICO_TECHNICAL'
        OR service_type LIKE 'PHARMACY'
        OR service_type LIKE 'ADMINISTRATIVE'
    );
