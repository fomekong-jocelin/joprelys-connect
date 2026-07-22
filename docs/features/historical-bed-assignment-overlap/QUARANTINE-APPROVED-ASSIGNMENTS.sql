-- HOS-BED-001-D
-- Procédure PostgreSQL 16 MANUELLE : ne jamais exécuter sans arbitrage DBA + bed manager.
-- Le script ne supprime aucune affectation. Il marque en quarantaine uniquement les IDs approuvés.
-- Par sécurité, l'exécution échoue tant que la table temporaire ne contient aucun ID explicite.

BEGIN;

CREATE TEMP TABLE approved_bed_assignment_quarantine (
    bed_assignment_id UUID PRIMARY KEY,
    reason VARCHAR(500) NOT NULL,
    approved_by VARCHAR(255) NOT NULL
) ON COMMIT DROP;

-- Exemple à remplacer par les décisions réellement signées :
-- INSERT INTO approved_bed_assignment_quarantine (bed_assignment_id, reason, approved_by)
-- VALUES
--     ('00000000-0000-0000-0000-000000000000',
--      'Correction rétroactive validée dans le dossier DBA-XXXX',
--      'bed.manager@etablissement.example');

DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM approved_bed_assignment_quarantine) THEN
        RAISE EXCEPTION
            'Aucun ID approuvé. Compléter approved_bed_assignment_quarantine après validation DBA et métier.';
    END IF;
END
$$;

DO $$
BEGIN
    IF EXISTS (
        SELECT 1
        FROM approved_bed_assignment_quarantine approved
        LEFT JOIN bed_assignments assignment
          ON assignment.id = approved.bed_assignment_id
        WHERE assignment.id IS NULL
           OR assignment.released_at IS NULL
           OR assignment.integrity_status <> 'VALID'
           OR LENGTH(TRIM(approved.reason)) = 0
           OR LENGTH(TRIM(approved.approved_by)) = 0
    ) THEN
        RAISE EXCEPTION
            'La quarantaine contient un ID absent, actif, déjà traité ou insuffisamment justifié.';
    END IF;
END
$$;

UPDATE bed_assignments assignment
SET integrity_status = 'QUARANTINED',
    quarantined_at = CURRENT_TIMESTAMP,
    quarantine_reason = approved.reason,
    quarantined_by = approved.approved_by
FROM approved_bed_assignment_quarantine approved
WHERE assignment.id = approved.bed_assignment_id;

-- Après l'arbitrage, aucune paire VALID ne doit encore se chevaucher.
DO $$
DECLARE
    unresolved_conflict_count BIGINT;
BEGIN
    SELECT COUNT(*)
    INTO unresolved_conflict_count
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
            '[)');

    IF unresolved_conflict_count > 0 THEN
        RAISE EXCEPTION
            'Quarantaine incomplète : % paire(s) VALID se chevauchent encore.',
            unresolved_conflict_count;
    END IF;
END
$$;

SELECT
    assignment.id,
    assignment.organization_id,
    assignment.bed_id,
    assignment.hospitalization_id,
    assignment.assigned_at,
    assignment.released_at,
    assignment.quarantined_at,
    assignment.quarantine_reason,
    assignment.quarantined_by
FROM bed_assignments assignment
JOIN approved_bed_assignment_quarantine approved
  ON approved.bed_assignment_id = assignment.id
ORDER BY assignment.assigned_at, assignment.id;

COMMIT;
