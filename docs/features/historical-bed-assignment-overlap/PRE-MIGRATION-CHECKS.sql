-- HOS-BED-001-D / V80
-- Préflight PostgreSQL 16 strictement en lecture seule.
-- Chaque requête de conflit doit retourner zéro ligne avant l'activation de V80.
-- Les bornes sont semi-ouvertes [début, fin) : fin A = début B est autorisé.

-- 1. Paires de périodes VALID qui se chevauchent sur le même lit et le même tenant.
WITH assignment_ranges AS (
    SELECT
        id,
        organization_id,
        bed_id,
        hospitalization_id,
        assigned_at,
        released_at,
        tsrange(
            assigned_at,
            COALESCE(released_at, 'infinity'::timestamp),
            '[)') AS assignment_period
    FROM bed_assignments
    WHERE integrity_status = 'VALID'
)
SELECT
    left_assignment.organization_id,
    left_assignment.bed_id,
    left_assignment.id AS left_assignment_id,
    left_assignment.hospitalization_id AS left_hospitalization_id,
    left_assignment.assigned_at AS left_assigned_at,
    left_assignment.released_at AS left_released_at,
    right_assignment.id AS right_assignment_id,
    right_assignment.hospitalization_id AS right_hospitalization_id,
    right_assignment.assigned_at AS right_assigned_at,
    right_assignment.released_at AS right_released_at,
    left_assignment.assignment_period * right_assignment.assignment_period AS overlap_period
FROM assignment_ranges left_assignment
JOIN assignment_ranges right_assignment
  ON left_assignment.organization_id = right_assignment.organization_id
 AND left_assignment.bed_id = right_assignment.bed_id
 AND left_assignment.id < right_assignment.id
 AND left_assignment.assignment_period && right_assignment.assignment_period
ORDER BY
    left_assignment.organization_id,
    left_assignment.bed_id,
    left_assignment.assigned_at,
    right_assignment.assigned_at;

-- 2. Synthèse par établissement et lit pour estimer le volume d'arbitrage.
WITH conflicting_pairs AS (
    SELECT
        left_assignment.organization_id,
        left_assignment.bed_id
    FROM bed_assignments left_assignment
    JOIN bed_assignments right_assignment
      ON left_assignment.organization_id = right_assignment.organization_id
     AND left_assignment.bed_id = right_assignment.bed_id
     AND left_assignment.id < right_assignment.id
    WHERE left_assignment.integrity_status = 'VALID'
      AND right_assignment.integrity_status = 'VALID'
      AND tsrange(
            left_assignment.assigned_at,
            COALESCE(left_assignment.released_at, 'infinity'::timestamp),
            '[)')
          &&
          tsrange(
            right_assignment.assigned_at,
            COALESCE(right_assignment.released_at, 'infinity'::timestamp),
            '[)')
)
SELECT
    organization_id,
    bed_id,
    COUNT(*) AS conflicting_pair_count
FROM conflicting_pairs
GROUP BY organization_id, bed_id
ORDER BY conflicting_pair_count DESC, organization_id, bed_id;

-- 3. Toute ligne déjà mise en quarantaine doit être clôturée et justifiée.
SELECT
    id AS bed_assignment_id,
    organization_id,
    bed_id,
    hospitalization_id,
    assigned_at,
    released_at,
    quarantined_at,
    quarantine_reason,
    quarantined_by
FROM bed_assignments
WHERE integrity_status = 'QUARANTINED'
  AND (
      released_at IS NULL
      OR quarantined_at IS NULL
      OR quarantine_reason IS NULL
      OR LENGTH(TRIM(quarantine_reason)) = 0
      OR quarantined_by IS NULL
      OR LENGTH(TRIM(quarantined_by)) = 0
  )
ORDER BY assigned_at, id;
