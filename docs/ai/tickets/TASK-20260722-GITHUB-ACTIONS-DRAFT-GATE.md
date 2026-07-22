# TASK-20260722 — Gate GitHub Actions sur les PR Draft

## Type

Diagnostic + Engineering CI/CD

## Contexte

La première optimisation de coût GitHub Actions a rendu les jobs backend/frontend sélectifs par stack. L'audit des PR récentes montre toutefois un rythme élevé de commits avant stabilisation : certaines PR dépassent 20, 30 ou 60 commits. Une CI distante complète sur chaque synchronisation peut donc consommer rapidement le quota, même avec `cancel-in-progress`.

## Objectif

Réserver la CI distante lourde aux PR réellement prêtes à être revues, sans diminuer le niveau de validation avant fusion.

## Décision

- PR en Draft : pas de job lourd GitHub Actions ;
- passage `Ready for review` : CI stricte automatique ;
- PR déjà Ready + nouveau commit : nouvelle CI stricte ;
- retour en Draft : arrêt des prochaines CI lourdes et annulation possible du run courant par `concurrency` ;
- pushes `main`/`develop` : CI maintenue.

## Critères d'acceptation

- [x] Ajouter les événements `ready_for_review` et `converted_to_draft` au workflow.
- [x] Conserver `opened`, `synchronize`, `reopened`.
- [x] Ne pas démarrer le job `changes` pour une PR Draft.
- [x] Conserver les pushes `main`/`develop`.
- [x] Conserver le filtrage backend/frontend.
- [x] Conserver Maven `clean verify` strict.
- [x] Conserver tests Angular + build production.
- [x] Conserver `concurrency.cancel-in-progress`.
- [ ] Valider qu'un commit sur cette PR Draft n'exécute aucun runner lourd.
- [ ] Passer la PR Ready et vérifier que backend + frontend s'exécutent, car le workflow lui-même est modifié.
- [ ] Fusionner uniquement après CI verte du head Ready.

## Audit

Exemples récents observés :

- PR #104 : 39 commits ;
- PR #103 : 27 commits ;
- PR #102 : 28 commits ;
- PR #101 : 37 commits ;
- PR #97 : 63 commits ;
- PR #96 : 43 commits ;
- PR #107 actuellement en Draft : 19 commits.

Le gain exact dépend du nombre de commits réalisés pendant la phase Draft. Dans le meilleur cas, des dizaines de validations distantes sont remplacées par une seule validation complète au passage Ready.

## Sécurité / qualité

Cette optimisation ne supprime aucun test. Elle modifie uniquement le moment où la CI distante lourde démarre. Les tests locaux restent attendus pendant le développement, puis la validation distante complète devient obligatoire avant revue/fusion.

## Risque

Une PR laissée en Draft peut ne jamais être validée à distance. La règle de gouvernance associée est donc obligatoire : `Draft = non fusionnable`; `Ready + CI verte du head courant = candidat à la fusion`.

## Estimation

- Profil : DevOps / senior full-stack
- Estimation : 0,25 j
- Reviewer : Tech Lead
- SemVer : aucun bump applicatif
