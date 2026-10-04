# TECHNICAL-DESIGN — HOS-ORG-001-A

## 1. Architecture

Nouveau bounded context léger `hospitalorganization` séparé de `spatial`.

Flux backend :

```text
Controller
  ↓
HospitalOrganizationUseCase
  ↓
DefaultHospitalOrganizationService
  ↓
Repositories
  ↓
PostgreSQL
```

Le frontend utilise un service API dédié et ne porte aucune règle métier de hiérarchie au-delà de l'affichage et du filtrage ergonomique des options. Le backend reste l'autorité finale.

## 2. Modèle

### HospitalServiceCatalogEntry

Référentiel global en lecture seule pour les établissements.

Champs :
- `code` stable ;
- `name_fr` ;
- `name_en` ;
- aucune catégorie `service_type` dans ce catalogue ; ce champ appartient à la table historique `hospital_services`, distincte de l'organisation hiérarchique ;
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
- `name varchar(120) nullable` ;
- `unit_type varchar(32)` : `POLE`, `DEPARTMENT`, `SERVICE`, `CARE_UNIT` ;
- `service_catalog_code varchar(64) nullable` ;
- `active boolean` ;
- timestamps.

Identité :
- depuis V113, pour `SERVICE`, `name` est facultatif ; un nom local de 2–120 caractères prime sur le libellé FR/EN du `service_catalog_code` obligatoire ;
- pour `POLE`, `DEPARTMENT`, `CARE_UNIT`, `name` est obligatoire et `service_catalog_code` est null.

Contraintes :
- unique `(organization_id, code)` ;
- unique `(id, organization_id)` pour FK composites ;
- parent du même tenant via FK composite ;
- FK catalogue restrictive ;
- check d'identité type/nom/catalogue ;
- pas de cascade destructive vers les faits métier futurs.

## 3. Validation de hiérarchie

La matrice est portée dans le service backend :

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
- code déjà utilisé dans le tenant ;
- nom local non vide de moins de 2 ou plus de 120 caractères pour un SERVICE ;
- catalogue sur un type autre que SERVICE.

## 4. Autorisation

Nouvelle permission : `ORGANIZATION_STRUCTURE_MANAGE`.

Rôles système cibles :
- `ADMIN_CLINIQUE` : oui ;
- `ADMIN_JOPRELYS` / `SUPER_ADMIN` : oui lorsqu'ils administrent un tenant explicitement sélectionné ;
- métiers cliniques : non par défaut.

Dans HOS-ORG-001-A, **lecture et mutation** des catalogues/unités utilisent cette permission de configuration. Les futurs formulaires métier qui auront besoin d'une lecture plus large utiliseront un contrat de lecture dédié dans HOS-STAFF/HOS-LOC plutôt que d'élargir silencieusement cette API d'administration.

`ORGANIZATION_STRUCTURE_MANAGE` reste clinic-scoped : elle n'appartient pas au jeu `platformPermissionCodes()`.

## 5. Isolation tenant

Défense en profondeur :

1. scope résolu depuis l'utilisateur ou l'administration plateforme explicite ;
2. `TenantContext` / `@TenantId` ;
3. méthodes repository prenant explicitement `organizationId` ;
4. FK composite `(parent_id, organization_id)` garantissant le parent du même tenant ;
5. `organizationId` absent du body métier.

## 6. Migration

Flyway V87 :

1. créer `hospital_service_catalog` ;
2. créer `medical_specialty_catalog` ;
3. créer `organizational_units` ;
4. ajouter index/FK/contraintes ;
5. seed catalogue initial FR/EN ;
6. ne pas migrer automatiquement les valeurs libres `department`/`specialty` ; leur remappage est explicitement porté par HOS-STAFF-001-A ;
7. ne pas créer automatiquement une unité par `Ward`, car `Ward` mélange organisation et géographie et une conversion automatique serait sémantiquement fausse.

## 7. Coexistence temporaire contrôlée

`Ward/Room/Bed` reste utilisé par l'hospitalisation jusqu'à HOS-LOC-001-A. Cette coexistence n'est pas une rétrocompatibilité finale : elle est bornée par #131/#132 et aucun nouveau code fonctionnel de personnel ne doit dépendre des champs texte libres.

## 8. API

Base : `/api/hospital-organization`.

- `GET /catalogs/services`
- `GET /catalogs/specialties`
- `GET /units?includeInactive=false`
- `POST /units`
- `PUT /units/{id}`
- `POST /units/{id}/deactivate`
- `POST /units/{id}/activate`

Pas de DELETE physique dans ce lot.

## 9. Angular

Feature : `clinic/hospital-organization`.

Implémentation actuelle :
- `HospitalOrganizationPageComponent` comme container autonome, maintenu sous la cible de 300 lignes ;
- `HospitalOrganizationApiService` ;
- modèles typés ;
- dictionnaire feature FR/EN ;
- route permission-first ;
- entrée de navigation conditionnée par `ORGANIZATION_STRUCTURE_MANAGE`.

La page reste volontairement simple dans ce lot. Elle sera extraite en sous-composants si HOS-LOC/HOS-STAFF augmente sa complexité, avant d'approcher la limite de 500 lignes.

Contraintes :
- Tailwind CSS v4 / tokens existants ;
- aucune couleur/URL/branding hardcodé ;
- i18n FR/EN ;
- un SERVICE n'a pas de libellé localisé stocké dans l'unité ;
- light/dark ;
- mobile empilé, desktop arborescent ;
- boutons sans retour à la ligne interne ;
- focus/labels natifs et modale utilisable sur petit écran.

## 10. Observabilité et audit

Création, mise à jour, activation et désactivation sont auditées via le service d'audit existant avec `organizationId`, acteur et unité. Aucun contenu clinique ou secret n'est ajouté aux logs.

## 11. Risques

- catalogue initial incomplet : extensible par changements contrôlés, pas par free text ;
- doublon temporaire avec `Ward` : interdit d'ajouter de nouvelles dépendances à `Ward` pour le staff ;
- future migration des unités vers espaces : HOS-LOC utilise UUID et relations datées, pas le nom du service ;
- l'administration plateforme doit toujours sélectionner explicitement un établissement avant de lire ou modifier les unités.

Mise à jour 2026-10-04 : voir docs/features/admin-panel-clinical-audit-fixes pour catalogues enrichis, gouvernance, migration V113 et preuves nouvelles.
