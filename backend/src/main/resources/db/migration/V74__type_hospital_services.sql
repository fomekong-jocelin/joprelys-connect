-- HOS-02 / #73 : le type métier du service devient obligatoire.
-- Aucune valeur par défaut ni déduction par le nom : les données ambiguës doivent être qualifiées avant déploiement.

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

-- La liste fermée est portée par l'enum du domaine et validée par l'API.
-- La contrainte SQL de valeur est volontairement omise ici pour éviter une divergence
-- de comportement entre PostgreSQL de production et H2 utilisé uniquement par les tests.
