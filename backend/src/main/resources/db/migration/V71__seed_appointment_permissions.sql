-- STORY-2601 (EPIC-0025) : seed des permissions du module rendez-vous.
-- Idempotent (INSERT ... SELECT ... WHERE NOT EXISTS, pattern V64), compatible H2/PostgreSQL.
-- Les liens rôle -> permission ne sont volontairement PAS seedés ici : ils sont possédés
-- par RbacCatalog et réappliqués à chaque démarrage par RbacStore.seedCatalog()
-- (DELETE puis réinsertion des role_permissions des rôles système).

INSERT INTO permissions (code, domain, name, description, created_at)
SELECT 'APPOINTMENT_READ', 'RENDEZ_VOUS', 'Consulter les rendez-vous',
       'Consulter l''agenda et les rendez-vous de l''établissement.', CURRENT_TIMESTAMP
WHERE NOT EXISTS (
    SELECT 1 FROM permissions WHERE code = 'APPOINTMENT_READ'
);

INSERT INTO permissions (code, domain, name, description, created_at)
SELECT 'APPOINTMENT_WRITE', 'RENDEZ_VOUS', 'Gérer les rendez-vous',
       'Réserver pour un patient, enregistrer les arrivées et gérer le cycle de vie des rendez-vous.', CURRENT_TIMESTAMP
WHERE NOT EXISTS (
    SELECT 1 FROM permissions WHERE code = 'APPOINTMENT_WRITE'
);

INSERT INTO permissions (code, domain, name, description, created_at)
SELECT 'AVAILABILITY_MANAGE', 'RENDEZ_VOUS', 'Gérer les disponibilités médecins',
       'Définir les plages de disponibilité récurrentes et les indisponibilités des médecins.', CURRENT_TIMESTAMP
WHERE NOT EXISTS (
    SELECT 1 FROM permissions WHERE code = 'AVAILABILITY_MANAGE'
);
