-- HOS-BED-001-B / V77
-- Préflight strictement en lecture seule. Chaque requête doit retourner zéro ligne.

-- 1. Affectations orphelines : à qualifier avec le DBA et le métier, sans suppression automatique.
SELECT
    ba.id AS bed_assignment_id,
    ba.hospitalization_id,
    ba.bed_id,
    ba.assigned_at,
    ba.released_at
FROM bed_assignments ba
LEFT JOIN hospitalizations h ON h.id = ba.hospitalization_id
WHERE h.id IS NULL
ORDER BY ba.assigned_at, ba.id;

-- 2. Plusieurs affectations actives pour un même séjour.
SELECT
    ba.hospitalization_id,
    COUNT(*) AS active_assignment_count,
    MIN(ba.assigned_at) AS first_assigned_at,
    MAX(ba.assigned_at) AS last_assigned_at
FROM bed_assignments ba
WHERE ba.released_at IS NULL
GROUP BY ba.hospitalization_id
HAVING COUNT(*) > 1
ORDER BY ba.hospitalization_id;

-- 3. Périodes dont la fin précède le début.
SELECT
    ba.id AS bed_assignment_id,
    ba.hospitalization_id,
    ba.bed_id,
    ba.assigned_at,
    ba.released_at
FROM bed_assignments ba
WHERE ba.released_at < ba.assigned_at
ORDER BY ba.assigned_at, ba.id;
