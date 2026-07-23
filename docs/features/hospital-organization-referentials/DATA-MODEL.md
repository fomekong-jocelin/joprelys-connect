# DATA-MODEL — HOS-ORG-001-A

## Tables

### `hospital_service_catalog`

| Colonne | Type | Règle |
|---|---|---|
| `code` | varchar(64) PK | code stable |
| `name_fr` | varchar(120) | obligatoire |
| `name_en` | varchar(120) | obligatoire |
| `service_type` | varchar(40) | catégorie fonctionnelle contrôlée |
| `active` | boolean | défaut true |

Catalogue global piloté par le produit, non éditable par l'établissement dans ce lot. `service_type` correspond à l'enum backend `HospitalServiceType` existante afin de ne pas créer une seconde taxonomie technique concurrente.

### `medical_specialty_catalog`

| Colonne | Type | Règle |
|---|---|---|
| `code` | varchar(64) PK | code stable |
| `name_fr` | varchar(120) | obligatoire |
| `name_en` | varchar(120) | obligatoire |
| `active` | boolean | défaut true |

### `organizational_units`

| Colonne | Type | Règle |
|---|---|---|
| `id` | uuid PK | généré applicativement |
| `organization_id` | uuid | tenant obligatoire |
| `parent_id` | uuid nullable | parent même tenant |
| `code` | varchar(64) | unique par tenant |
| `name` | varchar(120) nullable | nom propre pour POLE/DEPARTMENT/CARE_UNIT ; obligatoirement null pour SERVICE |
| `unit_type` | varchar(32) | POLE / DEPARTMENT / SERVICE / CARE_UNIT |
| `service_catalog_code` | varchar(64) nullable | requis uniquement pour SERVICE |
| `active` | boolean | défaut true |
| `created_at` | timestamp | obligatoire |
| `updated_at` | timestamp | obligatoire |

Le libellé d'un `SERVICE` n'est jamais persisté dans une langue particulière. Le client résout le nom FR/EN depuis `service_catalog_code` et le catalogue actif.

## Contraintes

- `UNIQUE (organization_id, code)` ;
- `UNIQUE (id, organization_id)` ;
- FK composite `(parent_id, organization_id)` → `(id, organization_id)` ;
- FK restrictive `service_catalog_code` → `hospital_service_catalog(code)` ;
- CHECK d'identité : SERVICE => catalogue obligatoire + `name IS NULL` ; autres types => catalogue null + `name` non vide ;
- CHECK : `code` non vide ;
- aucune suppression cascade vers l'organisation.

## Seed catalogue services

Codes initiaux :

`GENERAL_MEDICINE`, `INTERNAL_MEDICINE`, `MATERNITY_OBGYN`, `PEDIATRICS`, `EMERGENCY`, `GENERAL_SURGERY`, `CARDIOLOGY`, `INTENSIVE_CARE`, `ANESTHESIA`, `OPERATING_THEATRE`, `LABORATORY`, `IMAGING`, `PHARMACY`, `INPATIENT_GENERAL`.

## Seed spécialités

`GENERAL_MEDICINE`, `INTERNAL_MEDICINE`, `PEDIATRICS`, `OBSTETRICS_GYNECOLOGY`, `CARDIOLOGY`, `GENERAL_SURGERY`, `ANESTHESIA_CRITICAL_CARE`, `RADIOLOGY`, `MEDICAL_BIOLOGY`, `HOSPITAL_PHARMACY`.

## Migration des données existantes

Aucun mapping automatique depuis `wards.name`, `users.department` ou `users.specialty` : les valeurs historiques sont libres et ambiguës. Une migration automatique créerait de faux rattachements. Le remappage contrôlé sera réalisé dans HOS-LOC-001-A / HOS-STAFF-001-A avec preuves et règles explicites.
