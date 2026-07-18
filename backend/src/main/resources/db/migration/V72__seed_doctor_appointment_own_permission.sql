-- BUG-20260718 : permission dédiée à l'agenda personnel du médecin.
-- Idempotent et compatible H2/PostgreSQL. Les liens rôle -> permission sont
-- réappliqués par RbacStore.seedCatalog() à partir de RbacCatalog.

INSERT INTO permissions (code, domain, name, description, created_at)
SELECT 'APPOINTMENT_READ_OWN', 'RENDEZ_VOUS', 'Consulter son agenda médecin',
       'Consulter uniquement les rendez-vous affectés au médecin connecté.', CURRENT_TIMESTAMP
WHERE NOT EXISTS (
    SELECT 1 FROM permissions WHERE code = 'APPOINTMENT_READ_OWN'
);
