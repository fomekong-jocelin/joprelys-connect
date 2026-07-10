# BUG-20260710-CI-BASELINE-EXECUTION — Pipeline backend/frontend bloqué avant les tests

## Mode

Diagnostic + Engineering CI/CD.

**Statut :** QA — correction minimale terminée et workflow définitif validé dans la PR temporaire #11.

## Problème

Le pipeline GitHub Actions ne pouvait pas exécuter les suites de tests :

1. le job backend appelait `./mvnw`, mais le wrapper n’était pas exécutable sur le runner Linux et échouait avec `Permission denied` ;
2. le job frontend appelait `npm test -- --run`, ce qui devenait `ng test --run`, alors qu’Angular CLI 22 ne reconnaît pas l’option `--run`.

Ces erreurs survenaient avant l’exécution des tests applicatifs et rendaient l’état du dépôt non fiable.

## Objectif

Rétablir une baseline CI qui exécute réellement :

- le build Maven strict et les tests backend ;
- les tests Angular/Vitest ;
- le build Angular de production si les tests frontend réussissent.

## Périmètre inclus

- `.github/workflows/ci.yml` ;
- documentation du pipeline ;
- validation sur Pull Request.

## Périmètre exclu

- correction des tests applicatifs ensuite révélés ;
- modification du code Spring Boot ou Angular ;
- ajout de SonarQube, Checkmarx ou Nexus ;
- refonte complète du pipeline.

## Critères d’acceptation

- [x] Le wrapper Maven est rendu exécutable avant l’appel `./mvnw`.
- [x] Le job backend atteint réellement Maven et Flyway.
- [x] Le frontend n’utilise plus l’option Angular invalide `--run`.
- [x] `ng test` s’exécute en mode non-watch dans GitHub Actions.
- [x] Le build Angular reste exécuté uniquement après des tests réussis.
- [x] Aucun test n’est ignoré, neutralisé ou marqué comme réussi artificiellement.
- [x] Les échecs applicatifs découverts ont été tracés séparément.
- [x] Les étapes et artefacts temporaires de diagnostic ont été retirés du workflow définitif.

## Plan d’action

- [x] Reproduire les échecs dans une PR de diagnostic.
- [x] Extraire les logs backend et frontend.
- [x] Confirmer `./mvnw: Permission denied`.
- [x] Confirmer `Error: Unknown argument: run`.
- [x] Vérifier le comportement Angular 22 hors TTY.
- [x] Ajouter une étape `chmod +x mvnw` avant le build backend.
- [x] Remplacer `npm test -- --run` par `npm test`.
- [x] Exécuter la CI avec le workflow propre.
- [x] Documenter les échecs applicatifs révélés dans les tickets V55 et BigDecimal.
- [ ] Mettre à jour le changelog et le suivi projet avant fusion.
- [x] Ouvrir la Pull Request dédiée : #9.

## Definition of Ready

- [x] Causes techniques prouvées par les artefacts CI.
- [x] Périmètre limité au workflow.
- [x] Solution sans contournement de tests définie.
- [x] Critères d’acceptation écrits.

## Definition of Done

- [x] Les deux jobs atteignent leurs commandes de test réelles.
- [x] Le pipeline ne contient plus les deux erreurs d’invocation.
- [x] Les résultats réels des suites sont visibles.
- [x] Les diagnostics temporaires ont été supprimés.
- [x] Ticket et documentation à jour.
- [ ] Changelog et suivi finalisés avant fusion.

## Estimation et responsabilités

| Champ | Valeur |
|---|---|
| Priorité | P0 |
| Story points | 1 |
| Estimation senior | 0,15 j |
| Estimation intermédiaire | 0,25 j |
| Estimation junior | 0,5 j |
| Profil recommandé | DevOps / full-stack intermédiaire |
| Reviewer | Lead Developer |
| Sprint | SPRINT-0014 |

## Résultats de validation

Validation finale sur la PR temporaire #11 avec le workflow définitif :

- Backend Maven strict : ✅
- Tests backend : ✅ 276 tests
- Tests Angular : ✅
- Build Angular production : ✅

## Sécurité et régression

- Aucun secret, droit GitHub ou permission applicative n’est modifié.
- Les tests ne sont ni désactivés ni assouplis.
- `chmod +x` s’applique uniquement au wrapper versionné dans le workspace éphémère du runner.
- La commande Angular reste celle définie dans `package.json`.
- Aucun artefact de log temporaire ne reste dans le workflow final.

## Impact version

Aucun bump applicatif : correction de pipeline uniquement.

## Preuves de diagnostic

- Backend initial : `./mvnw: Permission denied`.
- Frontend initial : `Error: Unknown argument: run`.
- Workflow final : exécution verte des commandes réelles sans `continue-on-error`.

## Reste à faire

Finaliser le changelog et le suivi projet avant fusion de la PR #9.
