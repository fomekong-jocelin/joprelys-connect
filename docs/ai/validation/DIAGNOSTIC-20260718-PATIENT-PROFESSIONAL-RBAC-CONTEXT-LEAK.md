# Diagnostic — Fuite de contexte professionnel vers le portail patient

## Résumé exécutif

Le défaut principal est un cache RBAC Angular global non lié à la session authentifiée. Après une session professionnelle, `RbacApiService.access` conserve les permissions en mémoire. La connexion patient remplace le jeton dans `AuthTokenStorageService`, mais ne rend pas ce cache obsolète. `AppShellNavComponent` combine alors le rôle patient courant avec les anciennes permissions professionnelles et ajoute des routes cliniques au menu.

La route `/clinic/availability` possédait un guard et le contrôleur Spring une protection par rôle. L'analyse ne démontre donc pas un accès métier backend patient avant correction. Cependant, la fuite d'interface est un incident P0 de cloisonnement et révèle des défenses manquantes : cache non lié au jeton, permission contournable par le rôle dans le guard, absence de frontière patient explicite et copie de rôles mixtes par le filtre JWT patient.

## Preuves

### Cause racine frontend

- `web/src/app/clinic/rbac/rbac-api.service.ts` conserve `access` sans l'associer au jeton ayant produit la réponse.
- `web/src/app/auth/auth-token-storage.service.ts` remplace ou supprime la session, mais n'invalide pas ce cache.
- `web/src/app/shared/layout/app-shell-nav.component.ts` construit d'abord les menus professionnels depuis `rbacApi.access()?.permissions`, puis ajoute les menus patient. Une permission résiduelle `AVAILABILITY_MANAGE` ajoute `/clinic/availability`.
- `loadEffectiveAccess()` ne recharge volontairement pas `/api/rbac/me` pour un patient, ce qui laisse le cache professionnel intact.

### Défenses existantes

- `web/src/app/app.routes.ts` protège `/clinic/availability` par `roleGuard` avec rôles `MEDECIN` / `ADMIN_CLINIQUE`.
- `backend/.../appointment/api/AvailabilityController.java` protège chaque méthode avec `hasAnyRole('MEDECIN','ADMIN_CLINIQUE')`.
- `backend/.../appointment/api/PatientAppointmentController.java` impose `hasRole('PATIENT')` au niveau de la classe.

### Faiblesses connexes

- `roleGuard` consulte le RBAC distant pour une route à permissions avant d'appliquer une frontière patient explicite.
- `JwtAuthenticationFilter.isPatient()` choisit la branche patient dès qu'un claim contient `PATIENT`, puis transforme actuellement tous les rôles du claim en authorities. Un claim mixte valide pourrait donc produire `ROLE_PATIENT` et un rôle professionnel.
- `AvailabilityControllerTest` couvre un rôle accueil refusé, mais pas un vrai jeton patient.
- Le même cache pouvait contaminer n'importe quel changement de compte professionnel ; l'incident visible patient/médecin n'était qu'une manifestation.
- L'inventaire global compte 41 contrôleurs avec `@PreAuthorize` : 20 utilisent au moins une authority et 21 restent exclusivement fondés sur des rôles. Cette dette doit être migrée domaine par domaine avec une matrice validée.

## Scénario de reproduction

1. Se connecter avec un médecin et charger le shell afin que `/api/rbac/me` remplisse le cache.
2. Se déconnecter sans recharger complètement l'application.
3. Se connecter comme patient par OTP.
4. Le shell lit le rôle `PATIENT`, mais conserve les permissions du médecin dans `RbacApiService.access`.
5. Le menu ajoute à la fois les entrées patient et les entrées professionnelles autorisées par le cache, dont « Disponibilités ».

## Risque

- Sévérité : P0 / High — OWASP A01 Broken Access Control.
- Impact prouvé : exposition de navigation et d'interface professionnelle à un patient, confusion de contexte et perte de confiance.
- Impact backend : non prouvé sur `/api/availabilities/**` grâce aux annotations existantes ; doit être verrouillé et prouvé par tests.
- Portée : générique. Toute entrée de menu pilotée par une permission RBAC résiduelle peut être affichée, pas seulement les disponibilités.

## Correction retenue

1. Associer chaque cache/réponse RBAC au jeton courant et ignorer les réponses d'une ancienne session.
2. Construire un menu exclusivement patient dès que `PATIENT` est présent.
3. Refuser immédiatement toute route non-patient dans `roleGuard` pour une session patient.
4. Limiter la branche patient du filtre JWT à l'unique authority `ROLE_PATIENT`.
5. Ajouter des tests de cache résiduel, routes, rôle mixte et endpoint disponibilités.
6. Lorsqu'une route déclare `expectedPermissions`, exiger la permission sans fallback sur le rôle.
7. Exiger `AVAILABILITY_MANAGE` sur chaque endpoint de gestion des disponibilités.

## Régressions à surveiller

- Rafraîchissement de jeton professionnel : le RBAC sera rechargé, avec un coût réseau limité et acceptable.
- Rôles professionnels personnalisés : fonctionnement renforcé ; une route déclarant une permission ne peut plus être ouverte par simple correspondance de rôle.
- Portail patient et rendez-vous : les routes `/patient/**` restent accessibles et les API patient conservent `ROLE_PATIENT`.
- Navigation mobile et desktop : elles partagent `AppShellNavComponent`, donc la correction couvre les deux.

## Impact planning et version

- Effort correctif P0 : 0,75 j Senior, 3 SP, revue Tech Lead + QA sécurité.
- Généralisation : EPIC-0026, 21 SP estimés, migration progressive par domaine.
- Bump : PATCH, correctif rétrocompatible sans changement de contrat ni de base.

## Résolution du 2026-07-18

Le correctif P0 et la généralisation EPIC-0026 sont implémentés. Aucun contrôleur ne contient désormais une annotation `@PreAuthorize` fondée sur `hasRole`/`hasAnyRole`; les routes professionnelles Angular utilisent les permissions effectives. Les suites complètes sont vertes : 453 tests Maven et 267 tests Angular. La validation RSSI/métiers demeure une étape de gouvernance avant livraison.
