# BUG-20260724-SPATIAL-INPATIENT-SPACES-CLINICAL-READ — Consultation clinique des espaces d'hébergement et lits

## 1. Contexte & Problématique

Lorsqu'un médecin (ex. `Dr Alain MBARGA` avec le rôle `MEDECIN`) ou un soignant accède à l'écran **« Occupation des lits & Chambres »** (`/clinic/spatial`), la liste des espaces d'hébergement ne se chargeait pas et affichait à tort le message : *"Aucun espace d'hébergement configuré. Créez un Space compatible avec les lits depuis la configuration spatiale."*

### Cause racine :
L'écran Angular interrogeait l'endpoint `/api/spatial/configuration/spaces`. Or, tous les endpoints `/api/spatial/configuration/*` sont des routes d'administration d'infrastructure protégées par `@PreAuthorize("hasAuthority('SPATIAL_CONFIGURATION_MANAGE')")`. Les praticiens (qui possèdent la permission de consultation hospitalière `HOSPITALIZATION_READ` mais pas la permission d'administration `SPATIAL_CONFIGURATION_MANAGE`) recevaient une erreur `403 Forbidden`, ce qui laissait la liste des espaces vide.

## 2. Solution apportée

1. **Backend Spring Boot** :
   - Ajout de l'endpoint opérationnel de consultation `@GetMapping("/inpatient-spaces")` sur `SpatialController` (`/api/spatial/inpatient-spaces`), protégé par `@PreAuthorize("hasAnyAuthority('HOSPITALIZATION_READ', 'SPATIAL_CONFIGURATION_MANAGE')")`.
   - Cet endpoint retourne la liste des espaces physiques actifs disposant d'un profil d'hébergement (`inpatientProfile != null`).
   - Les endpoints de configuration `/api/spatial/configuration/*` restent 100% réservés à l'administration (`SPATIAL_CONFIGURATION_MANAGE`).

2. **Frontend Angular** :
   - Mise à jour de `SpatialApiService.listInpatientSpaces()` pour interroger l'endpoint `/api/spatial/inpatient-spaces`.
   - Utilisation de cet endpoint par `SpatialManagementPageComponent` lors de la consultation des lits par espace.

3. **Gouvernance & Tests** :
   - Aucune nouvelle permission RBAC créée (réutilisation stricte de `HOSPITALIZATION_READ`).
   - Tests backend (Surefire / Spring Security) et frontend (`vitest` + `ng build`) verts.

## 3. Statut

En cours (Branche : `fix/spatial-inpatient-spaces-clinical-read`).
