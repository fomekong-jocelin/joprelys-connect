# CI Actions Cost Optimization — Functional Spec

## Problème

Le pipeline GitHub Actions exécute systématiquement le backend Maven et le frontend Angular pour toute modification non documentaire ciblant `main` ou `develop`. Sur un monorepo, cela consomme des minutes CI même lorsqu'un changement ne touche qu'une seule stack. Le budget GitHub Actions a atteint son plafond mensuel.

## Objectif

Réduire la consommation GitHub Actions sans diminuer le niveau de validation de la partie réellement modifiée.

## Périmètre

Inclus :
- détecter si un changement touche `backend/**`, `web/**` ou le workflow CI lui-même ;
- n'exécuter le job backend que si le backend ou le workflow CI est impacté ;
- n'exécuter le job frontend que si le frontend ou le workflow CI est impacté ;
- conserver les tests stricts actuels (`./mvnw clean verify`, tests Angular et build production) ;
- conserver l'annulation des runs obsolètes via `concurrency` ;
- borner la durée maximale des jobs afin d'éviter un runner bloqué ;
- conserver l'exclusion des modifications purement documentaires.

Exclus :
- changement de fournisseur CI ;
- suppression de tests ;
- réduction du niveau de contrôle sécurité ou qualité ;
- modification des règles fonctionnelles de Joprelys Connect.

## Règles

1. Un changement backend seul ne doit pas lancer le job Angular.
2. Un changement frontend seul ne doit pas lancer le job Maven.
3. Une modification de `.github/workflows/ci.yml` doit lancer les deux jobs pour valider le pipeline.
4. Un changement touchant backend et frontend doit lancer les deux jobs.
5. Les tests existants ne sont ni supprimés ni affaiblis.
6. Les runs précédents du même PR/ref restent annulés lorsqu'un nouveau run démarre.
7. Un job anormalement long doit être interrompu par un timeout explicite.

## Critères d'acceptation

- [ ] Backend-only : job backend exécuté, job frontend ignoré.
- [ ] Frontend-only : job frontend exécuté, job backend ignoré.
- [ ] Full-stack : les deux jobs sont exécutés.
- [ ] Modification du workflow : les deux jobs sont exécutés.
- [ ] `./mvnw clean verify -B --no-transfer-progress -Dspring.profiles.active=test` reste la validation backend.
- [ ] `npm test` puis `npm run build` restent la validation frontend.
- [ ] Aucun changement applicatif, API, DB, RBAC ou UI.

## Risques

Le principal risque est un filtre de chemins trop restrictif. Pour le réduire, toute modification du workflow CI force l'exécution des deux stacks et les filtres restent limités aux deux racines techniques actuelles du dépôt.