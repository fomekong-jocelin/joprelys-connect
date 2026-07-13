# Modèle de données — Triage ABCDE

## Table `emergency_triage_assessments`

| Colonne | Type | Null | Description |
|---|---|---:|---|
| `id` | UUID | non | Identifiant immuable de l’évaluation |
| `organization_id` | UUID | non | Tenant Hibernate |
| `emergency_id` | UUID | non | Dossier d’urgence |
| `assessment_type` | VARCHAR(20) | non | `INITIAL` ou `REASSESSMENT` |
| `sequence_number` | INTEGER | non | Ordre strict dans le dossier |
| `triage_level` | VARCHAR(20) | non | Rouge, orange, jaune ou vert |
| `hemodynamic_status` | VARCHAR(50) | non | Choc, instable ou stable |
| `airway_status` | VARCHAR(32) | non | Axe A |
| `breathing_status` | VARCHAR(32) | non | Axe B |
| `circulation_status` | VARCHAR(32) | non | Axe C |
| `disability_status` | VARCHAR(32) | non | Axe D |
| `exposure_status` | VARCHAR(32) | non | Axe E |
| `bp_systolic` | INTEGER | oui | Pression systolique |
| `bp_diastolic` | INTEGER | oui | Pression diastolique |
| `heart_rate` | INTEGER | oui | Fréquence cardiaque |
| `respiratory_rate` | INTEGER | oui | Fréquence respiratoire |
| `oxygen_saturation` | INTEGER | oui | SpO2 en pourcentage |
| `temperature` | NUMERIC(4,2) | oui | Température en °C |
| `gcs_score` | INTEGER | oui | Glasgow 3 à 15 |
| `pain_score` | INTEGER | oui | Douleur 0 à 10 |
| `recommended_orientation` | VARCHAR(32) | oui | Orientation clinique recommandée |
| `clinical_notes` | TEXT | oui | Notes de synthèse |
| `assessed_at` | TIMESTAMP WITH TIME ZONE | non | Heure clinique |
| `assessed_by_user_id` | UUID | oui | Professionnel auteur |
| `created_at` | TIMESTAMP WITH TIME ZONE | non | Heure technique d’insertion |

## Contraintes

- PK sur `id`.
- FK `emergency_id → emergencies(id)` avec `ON DELETE CASCADE`.
- FK `assessed_by_user_id → users(id)` avec `ON DELETE SET NULL`.
- unicité `(organization_id, emergency_id, sequence_number)`.
- checks sur séquence positive, SpO2 0–100, Glasgow 3–15, douleur 0–10 et fréquence respiratoire positive.
- index `(organization_id, emergency_id, sequence_number)`.

## Reprise des données

La migration V64 insère une évaluation `INITIAL` pour chaque urgence existante :

- `id = emergencies.id` pour éviter une fonction UUID spécifique au moteur ;
- séquence `1` ;
- valeurs historiques du niveau, de l’état hémodynamique et des constantes ;
- ABCDE à `NOT_ASSESSED` ;
- `assessed_at = emergencies.created_at` ;
- auteur repris depuis `created_by_user_id`.

Aucune donnée clinique absente n’est extrapolée.

## Conservation

Le journal est append-only. Les opérations métier n’exposent ni UPDATE ni DELETE sur une évaluation. La suppression technique du dossier d’urgence supprime ses évaluations par cascade, conformément au comportement actuel des données d’urgence.