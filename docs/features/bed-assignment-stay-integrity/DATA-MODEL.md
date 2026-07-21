# Modèle de données — Intégrité séjour et période

## Évolution de `bed_assignments`

| Colonne | Type | Null | Rôle |
|---|---|---:|---|
| `hospitalization_id` | UUID | non | FK restrictive vers le séjour |
| `active_hospitalization_id` | UUID | oui | clé unique tant que l'affectation est active |
| `assigned_at` | timestamp | non | début |
| `released_at` | timestamp | oui | fin, nulle si active |

## Contraintes V77

- `fk_bed_assignments_hospitalization` : référence `hospitalizations(id)`, suppression restrictive.
- `ck_bed_assignments_active_hospitalization_consistency` : marqueur égal au séjour si actif, nul si clôturé.
- `ck_bed_assignments_assignment_period` : `released_at IS NULL OR released_at >= assigned_at`.
- `uq_bed_assignments_active_hospitalization` : une seule affectation active par séjour.

## Cardinalités garanties après V76 + V77

- `Bed 1 → 0..1 BedAssignment active` ;
- `Hospitalization 1 → 0..1 BedAssignment active` ;
- `Bed/Hospitalization 1 → 0..n BedAssignment clôturée`.

## Limites restantes

La cohérence tenant composite est ajoutée par V78/HOS-BED-001-C. Les intervalles clôturés peuvent encore se chevaucher et nécessitent une décision de migration PostgreSQL séparée.
