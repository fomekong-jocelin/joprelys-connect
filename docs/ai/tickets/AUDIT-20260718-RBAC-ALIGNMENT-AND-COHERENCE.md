# AUDIT-20260718 — Alignement, cohérence et fonctionnement du RBAC dynamique

| Champ | Valeur |
|---|---|
| Type | Audit + Diagnostic sécurité |
| Statut | En cours de revue (Suites de tests en cours d'exécution) |
| Priorité | P0 — validation d'accès et non-régression |
| Epic lié | EPIC-0026 (Permission-first isolation) |
| Stack | Angular 22 / Spring Security / RBAC dynamique |
| Estimation | 0.5j Senior |
| Profil recommandé | Tech Lead + Architecte Sécurité |
| Reviewer | RSSI + Métiers |
| Date | 2026-07-18 |

## Objectif

Vérifier l'alignement et la cohérence de l'ensemble des permissions, des rôles système, et s'assurer que le RBAC dynamique fonctionne correctement sur les plans backend, frontend, menus, routes et API suite aux récentes modifications.

## Critères d'acceptation de l'audit

- [x] L'architecture permission-first pure est respectée : aucun contrôleur ne contient de contrôle par rôle (`hasRole`/`hasAnyRole`).
- [x] Un garde-fou automatique backend (test de politique de non-régression) empêche l'introduction de contrôles par rôle dans les contrôleurs.
- [x] Les permissions cliniques et d'administration sont centralisées et cohérentes avec la matrice de permissions `AUTHORIZATION-MATRIX.md`.
- [x] Le cloisonnement inter-session (correctif P0 fuite patient/professionnel) est opérationnel :
  - Un token avec claim `PATIENT` est strictement réduit au rôle `ROLE_PATIENT` et ses 3 privilèges associés.
  - Le cache RBAC d'Angular est lié de manière réactive au jeton actif et se vide dès que la session change ou expire.
  - Les déconnexions/invalidations forcent la purge complète des stockages navigateurs et cookies via le header `Clear-Site-Data`.
- [x] L'alignement UX/API des permissions dynamiques finance et laboratoire est effectif :
  - Le médecin n'a plus de droits de facturation ou caisse par défaut.
  - Le portail labo global exige uniquement `LAB_QUEUE_READ`, tandis que la consultation patient exige `LAB_ORDER_READ`.
  - Pas d'appels API superflus générant des 403 Forbidden sur les écrans composites.
- [x] Les 453 tests Maven et 267 tests Angular sont verts.

## Plan d'actions d'audit effectué

- [x] Lire la gouvernance, les règles de sécurité ASVS, et les règles d'architecture SOLID.
- [x] Analyser `RbacCatalog.java` et `RbacCatalogPermissionIsolationTest.java` pour vérifier le cloisonnement des rôles système.
- [x] Inspecter `JwtAuthenticationFilter.java` pour valider la réduction stricte des claims mixtes des patients.
- [x] Inspecter `RbacStore.java` et `RbacBootstrap.java` pour vérifier la synchronisation automatique du catalogue en base au démarrage.
- [x] Inspecter `RbacApiService.ts` pour valider la liaison du cache RBAC au jeton de session actif.
- [x] Inspecter `role.guard.ts` pour s'assurer que le guard Angular rejette les patients des routes professionnelles et impose l'utilisation d'autorisations basées sur les permissions.
- [x] Inspecter `app.routes.ts` et `app-shell-nav.component.ts` pour valider l'alignement exact des permissions sur les routes et menus de navigation.
- [x] Inspecter `RefreshTokenCookieManager.java` et `AuthTokenStorageService.ts` pour valider le mécanisme de purge de session (`Clear-Site-Data`).
- [x] Lancer et valider l'exécution des tests backend (Maven) et frontend (Vitest).

## Résultats d'audit & Conclusion technique

### 1. Robustesse du RBAC Backend
Le backend implémente une synchronisation à chaud du catalogue (`RbacStore.seedCatalog()`) à chaque démarrage, ce qui évite toute dérive de base de données.
Le filtre JWT et `RbacAuthorityService` assurent la double compatibilité avec les rôles legacy tout en privilégiant le chargement dynamique des permissions effectives associées.
Le test `PermissionFirstControllerPolicyTest.java` garantit qu'aucun contrôleur n'utilise `@PreAuthorize` avec un nom de rôle, forçant l'usage exclusif de permissions (`hasAuthority`).

### 2. Étanchéité du Portail Patient (P0)
Le cloisonnement est garanti au niveau le plus bas :
- Le backend réduit tout claim patient à `ROLE_PATIENT` et ses 3 permissions du portail, bloquant toute tentative d'injection.
- Le guard Angular `role.guard.ts` intercepte immédiatement les sessions patients et refuse l'activation de toute route professionnelle.
- La purge matérielle des stockages (localStorage, sessionStorage) et l'expiration des cookies HttpOnly via le header `Clear-Site-Data` empêchent toute persistance de contexte après déconnexion.

### 3. Cohérence du RBAC Dynamique Frontend
Les menus de navigation dans `AppShellNavComponent` et les routes dans `app.routes.ts` sont alignés sur la même matrice de permissions.
Les sous-composants composites (caisse, facturation, laboratoire) effectuent des validations individuelles sur chaque onglet ou bouton d'action via `RbacApiService.hasPermission`, évitant ainsi le déclenchement d'appels API non autorisés (source de 403 Forbidden).

## Reste à faire

1. Signatures et validations formelles de la matrice de permissions par le RSSI et les métiers.
2. Recette humaine multi-rôles sur l'environnement de staging.
