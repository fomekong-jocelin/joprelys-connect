# BUG-20260710-CI-BASELINE-EXECUTION — Pipeline backend/frontend bloqué avant les tests

## Mode

Diagnostic + Engineering CI/CD.

**Statut :** IN_PROGRESS — causes confirmées, correction minimale en cours.

## Problème

Le pipeline GitHub Actions ne peut pas exécuter les suites de tests :

1. le job backend appelle `./mvnw`, mais le wrapper n’est pas exécutable sur le runner Linux et échoue avec `Permission denied` ;
2. le job frontend appelle `npm test -- --run`, ce qui devient `ng test --run`, alors qu’Angular CLI 22 ne reconnaît pas l’option `--run`.

Ces erreurs surviennent avant l’exécution des tests applicatifs et rendent l’état vert/rouge du dépôt non fiable.

## Objectif

Rétablir une baseline CI qui exécute réellement :

- le build Maven strict et les tests backend ;
- les tests Angular/Vitest ;
- le build Angular de production si les tests frontend réussissent.

## Périmètre inclus

- `.github/workflows/ci.yml` ;
- documentation du pipeline ;
- exécution de la CI sur une Pull Request dédiée.

## Périmètre exclu

- correction des tests applicatifs qui pourraient ensuite échouer ;
- modification du code Spring Boot ou Angular ;
- ajout de SonarQube, Checkmarx ou Nexus ;
- refonte complète du pipeline.

## Critères d’acceptation

- [ ] Le wrapper Maven est exécutable avant l’appel `./mvnw`.
- [ ] Le job backend atteint réellement Maven et Flyway.
- [ ] Le frontend n’utilise plus l’option Angular invalide `--run`.
- [ ] `ng test` s’exécute en mode non-watch dans GitHub Actions.
- [ ] Le build Angular reste exécuté uniquement après des tests réussis.
- [ ] Aucun test n’est ignoré, neutralisé ou marqué comme réussi artificiellement.
- [ ] Tout échec applicatif découvert après le déblocage est tracé séparément.

## Plan d’action

- [x] Reproduire les échecs dans une PR de diagnostic.
- [x] Extraire les logs backend et frontend.
- [x] Confirmer `./mvnw: Permission denied`.
- [x] Confirmer `Error: Unknown argument: run`.
- [x] Vérifier la documentation officielle Angular 22.
- [ ] Ajouter une étape `chmod +x mvnw` avant le build backend.
- [ ] Remplacer `npm test -- --run` par `npm test`.
- [ ] Exécuter la CI.
- [ ] Documenter les éventuels échecs applicatifs révélés.
- [ ] Mettre à jour le changelog et le suivi projet.
- [ ] Ouvrir la Pull Request.

## Definition of Ready

- [x] Causes techniques prouvées par les artefacts CI.
- [x] Périmètre limité au workflow.
- [x] Solution sans contournement de tests définie.
- [x] Critères d’acceptation écrits.

## Definition of Done

- [ ] Les deux jobs atteignent leurs commandes de test réelles.
- [ ] Le pipeline ne contient plus les deux erreurs d’invocation.
- [ ] Les résultats réels des suites sont visibles.
- [ ] Ticket, documentation, changelog et suivi mis à jour.
- [ ] PR ouverte et revue.

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

## Sécurité et régression

- Aucun secret, droit GitHub ou permission applicative n’est modifié.
- Les tests ne sont ni désactivés ni assouplis.
- `chmod +x` s’applique uniquement au wrapper versionné dans le workspace éphémère du runner.
- La commande Angular reste celle définie dans `package.json`.

## Impact version

Aucun bump applicatif : correction de pipeline uniquement.

## Preuves de diagnostic

- Backend : `/home/runner/...: ./mvnw: Permission denied`.
- Frontend : `Error: Unknown argument: run`.
- Angular CLI 22 : `watch` vaut `false` par défaut hors environnement TTY, donc `ng test` suffit en CI.

## Reste à faire

Appliquer le correctif minimal, exécuter le pipeline et créer des tickets distincts pour tout échec applicatif réel ensuite révélé.