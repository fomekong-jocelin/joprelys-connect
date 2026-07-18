# BUG-20260718 — Fuite de contexte RBAC professionnel vers une session patient

| Champ | Valeur |
|---|---|
| Type | Diagnostic + Engineering sécurité |
| Statut | QA technique verte — revue RSSI/métiers en attente |
| Priorité | P0 — contrôle d'accès |
| Epic lié | EPIC-0025 (incident) / EPIC-0026 (généralisation) |
| Stack | Angular 22 / Spring Security / RBAC |
| Estimation | 3 SP / 0,75 j Senior |
| Profil recommandé | Senior full-stack sécurité |
| Reviewer | Tech Lead + QA sécurité |
| Date | 2026-07-18 |
| Rapport diagnostic | `docs/ai/validation/DIAGNOSTIC-20260718-PATIENT-PROFESSIONAL-RBAC-CONTEXT-LEAK.md` |

## Problème

Après utilisation d'un compte professionnel puis déconnexion, une connexion patient dans la même application peut réutiliser le cache Angular `RbacApiService.access`. Le menu patient affiche alors des entrées professionnelles, notamment `/clinic/availability`, donnant l'impression que le patient dispose de l'interface et de la navigation médecin.

Le backend protégeait `/api/availabilities/**` par rôle, mais la fuite d'interface viole le principe de moindre privilège. Le défaut de cache est transversal : un changement `CAISSIER → INFIRMIER`, `MEDECIN → PATIENT` ou toute autre permutation pouvait conserver temporairement les permissions de la session précédente.

## Critères d'acceptation

- [x] Une session contenant le rôle `PATIENT` n'affiche que la navigation patient, même si un cache RBAC professionnel résiduel existe.
- [x] Un patient ne peut activer aucune route professionnelle, même si une réponse RBAC obsolète contient des rôles ou permissions professionnels.
- [x] Le cache RBAC est lié au jeton qui l'a chargé et devient inutilisable dès que le jeton change, pour tous les rôles.
- [x] Une déconnexion purge `sessionStorage`, `localStorage`, les cookies accessibles et les états mémoire enregistrés ; le serveur expire le cookie HttpOnly et envoie `Clear-Site-Data`.
- [x] Une connexion sous une autre identité purge l'état du compte précédent avant d'enregistrer la nouvelle session.
- [x] Le backend n'accorde que `ROLE_PATIENT` à tout jeton patient, même si son claim de rôles est mixte.
- [x] Les endpoints `/api/availabilities/**` exigent `AVAILABILITY_MANAGE`, la gestion d'un autre praticien exige en plus `AVAILABILITY_MANAGE_ALL`, et les tests prévoient `403` pour un vrai jeton patient et un claim mixte.
- [x] Les endpoints `/api/patient/appointments/**` restent protégés par `ROLE_PATIENT` et le test existant refuse un médecin.
- [x] Les routes professionnelles voisines sont couvertes par le même cloisonnement générique frontend.
- [x] Les tests Angular et Maven ciblés, puis les suites complètes pertinentes, sont verts.

## Action plan

- [x] Lire la gouvernance, les standards sécurité et la documentation EPIC-0025.
- [x] Analyser le menu, le guard Angular, le cache RBAC, le filtre JWT et les contrôleurs rendez-vous.
- [x] Documenter le diagnostic et les risques de régression.
- [x] Lier le cache RBAC au jeton de session et neutraliser les réponses asynchrones obsolètes.
- [x] Rendre la navigation patient exclusive.
- [x] Ajouter une barrière patient explicite dans `roleGuard`.
- [x] Durcir le filtre JWT pour ne produire que `ROLE_PATIENT` en mode patient.
- [x] Rendre obligatoires les permissions déclarées par les routes Angular.
- [x] Passer `/api/availabilities/**` sur `AVAILABILITY_MANAGE` et séparer le périmètre global avec `AVAILABILITY_MANAGE_ALL`.
- [x] Ajouter les tests frontend/backend de non-régression et de rôles mixtes.
- [x] Auditer les routes Angular et endpoints du module rendez-vous.
- [x] Créer EPIC-0026 pour la migration permission-first de tous les domaines.
- [x] Purger les stockages/cookies/états mémoire à la déconnexion et lors d'un changement d'identité.
- [x] Ajouter `Clear-Site-Data` aux sorties de session et au passage professionnel → patient.
- [x] Exécuter les tests Angular complets, build, i18n et vérifications de configuration.
- [x] Mettre à jour changelog, suivi global et checklist finale.
- [x] Exécuter les tests Maven : 453 tests, 0 échec, 1 ignoré.

## Impacts

- API / DB : aucun payload ni migration ; configuration durcie en supprimant la valeur par défaut de la clé laboratoire existante.
- UI : suppression des entrées professionnelles indûment visibles pour un patient.
- Sécurité : correction OWASP A01 Broken Access Control, défense en profondeur frontend + backend.
- Planning : correctif P0 borné ; revue sécurité obligatoire avant livraison.
- SemVer : PATCH rétrocompatible.

## Reste à faire

Obtenir la revue Tech Lead/RSSI et la recette multi-rôles métier. La migration globale EPIC-0026 est implémentée techniquement ; elle reste en QA jusqu'à ces signatures.
