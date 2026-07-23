# TECHNICAL-DESIGN — HOS-ORG-001-A

## 1. Architecture

Nouveau bounded context léger `hospitalorganization` séparé de `spatial`.

Flux backend :

```text
Controller
  ↓
HospitalOrganizationService
  ↓
Repositories
  ↓
PostgreSQL
```

Le frontend utilise un service API dédié et ne porte aucune règle métier de hiérarchie au-delà de l'affichage.

## 2. Modèle

### ServiceCatalogEntry

Référentiel global en lecture seule pour les établissements.

Champs :
- `code` stable ;
- `name_fr` ;
- `name_en` ;
- `service_type` compatible avec le domaine actuel (`HOSPITALIZATION`, `EMERGENCY`, `OUTPATIENT`, `MEDICO_TECHNICAL`, `PHARMACY`, `ADMINISTRATIVE`) ;
- `active`.

### MedicalSpecialtyCatalogEntry

Champs :
- `code` stable ;
- `name_fr` ;
- `name_en` ;
- `active`.

### OrganizationalUnit

Champs :
- `id UUID` ;
- `organization_id UUID` tenant ;
- `parent_id UUID nullable` ;
- `code varchar(64)` ;
- `name varchar(120)` ;
- `unit_type varchar(32)` : `POLE`, `DEPARTMENT`, `SERVICE`, `CARE_UNIT` ;
- `service_catalog_code varchar(64) nullable` ;
- `active boolean` ;
- timestamps.

Contraintes :
- unique `(organization_id, code)` ;
- unique `(id, organization_id)` pour FK composites ;
- parent du même tenant via FK composite ;
- `service_catalog_code` obligatoire pour `SERVICE`, null pour les autres types ;
- FK catalogue restrictive ;
- pas de cascade destructive vers les faits métier futurs.

## 3. Validation de hiérarchie

La matrice est portée dans le domaine/service backend :

- racine : POLE, DEPARTMENT, SERVICE ;
- POLE : DEPARTMENT, SERVICE ;
- DEPARTMENT : SERVICE ;
- SERVICE : CARE_UNIT ;
- CARE_UNIT : aucun enfant.

Le backend refuse :
- cycle ;
- parent d'un autre tenant ;
- parent inactif pour une nouvelle unité ;
- niveau incompatible ;
- code déjà utilisé.

## 4. Autorisation

Nouvelle permission : `ORGANIZATION_STRUCTURE_MANAGE`.

Rôles système cibles :
- `ADMIN_CLINIQUE` : oui ;
- `ADMIN_JOPRELYS` / `SUPER_ADMIN` : selon administration d'un tenant via mécanisme plateforme existant ;
- métiers cliniques : non par défaut.

Lecture de la structure : endpoint accessible aux utilisateurs professionnels authentifiés du tenant lorsqu'elle sert à remplir un formulaire métier ; les mutations restent réservées à la permission dédiée.

## 5. Migration

Flyway V87 :

1. créer `hospital_service_catalog` ;
2. créer `medical_specialty_catalog` ;
3. créer `organizational_units` ;
4. ajouter index/FK/contraintes ;
5. seed catalogue initial ;
6. ne pas migrer automatiquement les valeurs libres `department`/`specialty` ; leur remappage est explicitement porté par HOS-STAFF-001-A ;
7. ne pas créer automatiquement un OrganizationalUnit par `Ward`, car `Ward` mélange organisation et géographie et une conversion automatique serait sémantiquement fausse.

## 6. Coexistence temporaire contrôlée

`Ward/Room/Bed` reste utilisé par l'hospitalisation jusqu'à HOS-LOC-001-A. Cette coexistence n'est pas une rétrocompatibilité finale : elle est bornée par les tickets #131 et #132 et aucun nouveau code fonctionnel ne doit dépendre des champs texte libres.

## 7. API

Base : `/api/hospital-organization`.

- `GET /catalogs/services`
- `GET /catalogs/specialties`
- `GET /units?includeInactive=false`
- `POST /units`
- `PUT /units/{id}`
- `POST /units/{id}/deactivate`
- `POST /units/{id}/activate`

Pas de DELETE physique dans ce lot.

## 8. Angular

Feature : `clinic/hospital-organization`.

Composants prévus :
- page container ;
- `organization-unit-tree` ;
- `organization-unit-form` ;
- `catalog-select` réutilisable si aucun composant existant ne couvre le besoin.

Contraintes :
- composants < 300 lignes cible, 500 max ;
- Tailwind CSS v4 / tokens existants ;
- aucune couleur/URL/branding hardcodé ;
- i18n FR/EN ;
- light/dark ;
- mobile empilé, desktop arborescent ;
- boutons sans retour à la ligne interne.

## 9. Observabilité et audit

Mutations auditées via le service d'audit existant si compatible : création, mise à jour, activation, désactivation, avec `organizationId`, actor et unit id. Aucun contenu clinique/PII supplémentaire dans les logs.

## 10. Risques

- catalogue initial incomplet : extensible par changements contrôlés, pas par free text ;
- doublon temporaire avec `Ward` : interdit d'ajouter de nouvelles dépendances à `Ward` pour le staff ;
- future migration des unités vers espaces : HOS-LOC utilise UUID et relations datées, pas le nom du service.
