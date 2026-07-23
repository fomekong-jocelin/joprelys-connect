# DATA-MODEL — HOS-LOC-001-A

## 1. `facility_location_node_type_catalog`

| Colonne | Type | Règle |
|---|---|---|
| `code` | varchar(32) PK | SITE / BUILDING / FLOOR / ZONE |
| `rank` | int | 10 / 20 / 30 / 40 |

Le rang permet au backend de valider les niveaux sautés sans hardcoder un parent unique obligatoire.

## 2. `facility_location_nodes`

| Colonne | Type | Règle |
|---|---|---|
| `id` | uuid PK | stable |
| `organization_id` | uuid | tenant obligatoire |
| `parent_id` | uuid nullable | même tenant |
| `code` | varchar(64) | unique par tenant |
| `name` | varchar(120) | nom d'instance |
| `node_type` | varchar(32) | FK catalogue |
| `active` | boolean | défaut true |
| `created_at` | timestamp tz | obligatoire |
| `updated_at` | timestamp tz | obligatoire |

Contraintes :

- `UNIQUE (organization_id, code)` ;
- `UNIQUE (id, organization_id)` ;
- FK composite `(parent_id, organization_id)` ;
- FK `node_type` vers catalogue ;
- nom/code non vides ;
- pas de cascade destructive.

## 3. `space_type_catalog`

| Colonne | Type | Règle |
|---|---|---|
| `code` | varchar(64) PK | stable |
| `name_fr` | varchar(120) | obligatoire |
| `name_en` | varchar(120) | obligatoire |
| `active` | boolean | défaut true |

Seed initial :

`CONSULTATION_ROOM`, `TREATMENT_ROOM`, `EMERGENCY_BOX`, `WAITING_ROOM`, `HOSPITAL_ROOM`, `OPERATING_ROOM`, `RECOVERY_ROOM`, `ICU_ROOM`, `DELIVERY_ROOM`, `NEONATAL_ROOM`, `LABORATORY_ROOM`, `IMAGING_ROOM`, `PHARMACY`, `STORAGE`, `OFFICE`, `MORGUE`, `OTHER_CONTROLLED`.

## 4. `inpatient_space_type_catalog`

Sous-référentiel garantissant les types compatibles avec `Bed` :

| Colonne | Type | Règle |
|---|---|---|
| `space_type_code` | varchar(64) PK | FK `space_type_catalog(code)` |

Seed A : `HOSPITAL_ROOM`, `ICU_ROOM`.

Cette table évite un booléen applicatif non garanti en DB.

## 5. `facility_spaces`

| Colonne | Type | Règle |
|---|---|---|
| `id` | uuid PK | stable |
| `organization_id` | uuid | tenant obligatoire |
| `location_node_id` | uuid nullable | même tenant |
| `code` | varchar(64) | unique par tenant |
| `name` | varchar(120) | instance physique |
| `space_type_code` | varchar(64) | FK catalogue |
| `active` | boolean | défaut true |
| `created_at` | timestamp tz | obligatoire |
| `updated_at` | timestamp tz | obligatoire |

Contraintes :

- unique `(organization_id, code)` ;
- unique `(id, organization_id)` ;
- unique `(id, organization_id, space_type_code)` pour profil d'hébergement ;
- FK composite localisation/tenant ;
- FK type espace restrictive.

## 6. `inpatient_space_profiles`

| Colonne | Type | Règle |
|---|---|---|
| `space_id` | uuid PK | 1:1 avec space |
| `organization_id` | uuid | tenant |
| `space_type_code` | varchar(64) | type recopié pour FK forte |
| `created_at` | timestamp tz | obligatoire |
| `updated_at` | timestamp tz | obligatoire |

Contraintes :

- `UNIQUE (space_id, organization_id)` ;
- FK composite `(space_id, organization_id, space_type_code)` vers `facility_spaces` ;
- FK `space_type_code` vers `inpatient_space_type_catalog`.

Un lit peut donc référencer uniquement un espace possédant réellement ce profil.

## 7. `organizational_unit_space_assignments`

| Colonne | Type | Règle |
|---|---|---|
| `id` | uuid PK | stable |
| `organization_id` | uuid | tenant |
| `organizational_unit_id` | uuid | FK V87 |
| `space_id` | uuid | FK space |
| `valid_from` | timestamp tz | obligatoire |
| `valid_to` | timestamp tz nullable | fin exclusive |
| `created_at` | timestamp tz | obligatoire |
| `updated_at` | timestamp tz | obligatoire |

Contraintes :

- unit même tenant ;
- space même tenant ;
- `valid_to IS NULL OR valid_to > valid_from` ;
- index recherche active ;
- PostgreSQL exclusion temporelle ajoutée par migration Java dédiée pour le même couple unit/space.

Le partage d'un espace entre différentes unités est autorisé.

## 8. `beds` — refactor

Ancien :

```text
beds.room_id → rooms → wards
```

Nouveau :

| Colonne | Évolution |
|---|---|
| `space_id` | ajouté, NOT NULL, FK profil hébergement |
| `room_id` | supprimé |
| `bed_number` | conservé |
| `status` | conservé temporairement comme projection legacy interne déjà gérée |
| `capacity_status` | conservé |
| `readiness_status` | conservé |
| `organization_id` | conservé |
| `version` | conservé |

Contraintes :

- unique `(organization_id, space_id, bed_number)` ;
- unique `(id, organization_id)` ;
- unique `(id, space_id, organization_id)` pour FK hospitalisation ;
- FK `(space_id, organization_id)` → inpatient profile.

## 9. `hospitalizations` — références structurées

Nouvelles colonnes :

| Colonne | Type | Rôle |
|---|---|---|
| `current_service_unit_id` | uuid NOT NULL | unité de prise en charge courante |
| `current_space_id` | uuid NOT NULL | espace courant |
| `current_bed_id` | uuid NOT NULL | lit courant |

Renommages :

| Ancien | Nouveau | Rôle |
|---|---|---|
| `service_name` | `service_name_snapshot` | snapshot lisible |
| `room_number` | `space_name_snapshot` | snapshot lisible |
| `bed_number` | `bed_number_snapshot` | snapshot lisible |

Contraintes :

- service unit même tenant ;
- space même tenant ;
- FK composite `(current_bed_id, current_space_id, organization_id)` → `beds(id, space_id, organization_id)` ;
- les snapshots ne participent à aucune FK/recherche d'identité.

## 10. Suppressions V89

Après le preflight V88 attestant l'absence de données legacy incompatibles :

- drop `rooms` ;
- drop `wards` ;
- drop anciens index V69 associés ;
- `room_id` retiré de `beds` ;
- aucune table alias créée.

## 11. Migration des données

Il n'existe **aucun mapping automatique** :

- `Ward.name` → OrganizationalUnit : interdit ;
- `Room.roomNumber` → FacilitySpace : interdit ;
- `hospitalizations.service_name` → serviceUnitId : interdit ;
- `hospitalizations.room_number` → spaceId : interdit.

Raison : les chaînes ne prouvent ni identité, ni tenant relationnel, ni sémantique métier.

Une base non vide doit être reset/remappée par une opération contrôlée distincte avant d'appliquer le breaking cleanup.
