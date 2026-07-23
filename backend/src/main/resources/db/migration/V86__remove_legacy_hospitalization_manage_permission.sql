-- HOS-RBAC-001-D
-- La permission générique HOSPITALIZATION_MANAGE est définitivement retirée.
-- La FK role_permissions(permission_code) -> permissions(code) est ON DELETE CASCADE
-- depuis V57 : les affectations legacy disparaissent sans attribuer automatiquement
-- de nouvelles permissions plus larges aux rôles personnalisés.

DELETE FROM permissions
WHERE code = 'HOSPITALIZATION_MANAGE';
