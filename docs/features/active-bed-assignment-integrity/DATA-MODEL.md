# Modèle de données — Intégrité des affectations actives de lit

## Table concernée

### `bed_assignments`

| Colonne | Type | Null | Rôle |
|---|---|---:|---|
| `id` | UUID | non | identité de l'affectation |
| `hospitalization_id` | UUID | non | séjour associé, FK différée à un incrément ultérieur |
| `bed_id` | UUID | non | lit historique affecté |
| `assigned_at` | timestamp | non | début de l'affectation |
| `released_at` | timestamp | oui | fin ; nul signifie actif |
| `active_bed_id` | UUID | oui | clé technique unique tant que l'affectation est active |
| `organization_id` | UUID | non | tenant |

## Contraintes ajoutées

- `ck_bed_assignments_active_bed_consistency` :
  - si `released_at IS NULL`, `active_bed_id` est non nul et égal à `bed_id` ;
  - sinon `active_bed_id IS NULL`.
- `uq_bed_assignments_active_bed` : unicité de `active_bed_id`.

## Cardinalité garantie

`Bed 1 → 0..1 BedAssignment active` et `Bed 1 → 0..n BedAssignment clôturée`.

## Historisation

Aucune ligne historique n'est supprimée. La clôture renseigne `released_at` et annule uniquement la clé technique active.

## Dette restante

La base ne garantit pas encore l'absence de chevauchement de périodes clôturées ni la présence du séjour référencé. Ces règles feront l'objet de migrations séparées avec réconciliation dédiée.
