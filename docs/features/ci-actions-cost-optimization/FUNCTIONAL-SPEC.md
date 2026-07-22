# CI Actions Cost Optimization — Functional Spec

## Problème

Le pipeline GitHub Actions peut consommer rapidement le quota mensuel lorsque :

- backend et frontend sont exécutés alors qu'une seule stack change ;
- une PR en développement actif reçoit de nombreux commits et relance une CI lourde à chaque synchronisation.

Les PR récentes montrent plusieurs branches avec des dizaines de commits avant stabilisation, ce qui rend le deuxième cas significatif.

## Objectif

Réduire la consommation GitHub Actions sans diminuer le niveau de validation exigé avant revue et fusion.

## Périmètre

Inclus :
- détecter si un changement touche `backend/**`, `web/**` ou le workflow CI lui-même ;
- n'exécuter le job backend que si le backend ou le workflow CI est impacté ;
- n'exécuter le job frontend que si le frontend ou le workflow CI est impacté ;
- ne pas lancer les jobs lourds sur une PR encore en Draft ;
- déclencher automatiquement la CI stricte au passage `Ready for review` ;
- continuer à relancer la CI sur les nouveaux commits d'une PR déjà Ready ;
- conserver la validation des pushes sur `main` et `develop` ;
- conserver les tests stricts actuels (`./mvnw clean verify`, tests Angular et build production) ;
- conserver l'annulation des runs obsolètes via `concurrency` ;
- borner la durée maximale des jobs ;
- conserver l'exclusion des modifications purement documentaires.

Exclus :
- changement de fournisseur CI ;
- suppression de tests ;
- réduction du niveau de contrôle sécurité ou qualité ;
- fusion d'une PR restée en Draft ;
- modification des règles fonctionnelles de Joprelys Connect.

## Règles

1. Un changement backend seul ne doit pas lancer le job Angular.
2. Un changement frontend seul ne doit pas lancer le job Maven.
3. Une modification de `.github/workflows/ci.yml` doit lancer les deux jobs lorsque la PR est Ready.
4. Un changement touchant backend et frontend doit lancer les deux jobs lorsque la PR est Ready.
5. Une PR Draft peut recevoir des commits sans lancer de runner lourd.
6. Le passage Draft → Ready for review doit déclencher automatiquement la validation distante du head courant.
7. Une PR Ready qui reçoit un nouveau commit doit être revalidée.
8. Le retour Ready → Draft doit empêcher les prochains commits de déclencher les jobs lourds et permettre à `concurrency` d'annuler un run encore actif.
9. Les pushes sur `main`/`develop` restent validés.
10. Les tests existants ne sont ni supprimés ni affaiblis.
11. Aucun merge ne doit être réalisé directement depuis Draft sans CI verte du head courant.

## Critères d'acceptation

- [ ] Draft + commit backend : aucun job lourd.
- [ ] Draft + commit frontend : aucun job lourd.
- [ ] Passage Ready d'une PR backend-only : backend exécuté, frontend ignoré.
- [ ] Passage Ready d'une PR frontend-only : frontend exécuté, backend ignoré.
- [ ] Passage Ready d'une PR full-stack : deux jobs exécutés.
- [ ] Modification du workflow + Ready : deux jobs exécutés.
- [ ] Commit supplémentaire sur une PR Ready : nouvelle CI déclenchée.
- [ ] Push `main`/`develop` : CI déclenchée normalement.
- [ ] `./mvnw clean verify -B --no-transfer-progress -Dspring.profiles.active=test` reste la validation backend.
- [ ] `npm test` puis `npm run build` restent la validation frontend.
- [ ] Aucun changement applicatif, API, DB, RBAC ou UI.

## Risques

Le principal risque organisationnel est qu'une PR reste en Draft et soit considérée à tort comme validée. La règle de delivery doit donc rester explicite : **Draft = développement en cours ; Ready for review + CI verte = candidat à la revue/fusion.**
