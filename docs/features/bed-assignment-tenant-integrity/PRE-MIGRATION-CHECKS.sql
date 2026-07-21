-- HOS-BED-001-C / V78
-- Préflight strictement en lecture seule. Chaque requête doit retourner zéro ligne.

-- 1. Affectations dont l'établissement diffère du séjour.
SELECT
    ba.id AS bed_assignment_id,
    ba.organization_id AS assignment_organization_id,
    h.id AS hospitalization_id,
    h.organization_id AS hospitalization_organization_id
FROM bed_assignments ba
JOIN hospitalizations h ON h.id = ba.hospitalization_id
WHERE ba.organization_id <> h.organization_id
ORDER BY ba.assigned_at, ba.id;

-- 2. Affectations dont l'établissement diffère du lit.
SELECT
    ba.id AS bed_assignment_id,
    ba.organization_id AS assignment_organization_id,
    b.id AS bed_id,
    b.organization_id AS bed_organization_id
FROM bed_assignments ba
JOIN beds b ON b.id = ba.bed_id
WHERE ba.organization_id <> b.organization_id
ORDER BY ba.assigned_at, ba.id;
