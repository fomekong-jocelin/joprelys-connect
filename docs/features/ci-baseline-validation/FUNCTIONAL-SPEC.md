# CI Baseline Validation — Functional Specification

## Problème

Une Pull Request ne peut pas être considérée comme sûre si le pipeline échoue avant d’exécuter les tests. Les erreurs d’invocation actuelles masquent l’état réel du backend et du frontend.

## Utilisateurs concernés

- développeurs ;
- reviewers ;
- QA ;
- responsables de livraison ;
- agents IA intervenant sur le dépôt.

## Objectif

Chaque Pull Request vers `main` ou `develop` doit exécuter effectivement les validations backend et frontend prévues par le dépôt.

## Parcours attendu

```text
Pull Request
→ checkout
→ environnement Java / Node
→ tests backend Maven
→ tests frontend Angular/Vitest
→ build Angular
→ résultat CI exploitable pour la review
```

## Règles

1. Un problème de permission du wrapper ne doit pas empêcher Maven de démarrer.
2. La CI utilise la commande Maven Wrapper du dépôt, pas une installation Maven divergente.
3. La CI Angular utilise uniquement des options reconnues par la version Angular du projet.
4. Aucun test ne doit être désactivé pour obtenir un pipeline vert.
5. Un échec applicatif découvert après le déblocage reste bloquant et doit être corrigé dans un ticket distinct.
6. Le build de production Angular n’est exécuté qu’après le succès des tests frontend.

## Périmètre inclus

- exécution du wrapper Maven sur Ubuntu ;
- exécution non interactive d’Angular/Vitest ;
- conservation des étapes de build existantes.

## Périmètre exclu

- qualité interne des tests ;
- couverture ;
- scans de sécurité ;
- déploiement ;
- publication d’artefacts.

## Critères d’acceptation

- Maven démarre dans le job backend.
- Angular CLI accepte la commande du job frontend.
- Les suites réelles sont exécutées.
- Le pipeline expose les vrais échecs applicatifs, sans faux positif.
- Aucun code produit n’est modifié.

## Cas limites

- wrapper sans bit exécutable après checkout Linux ;
- CI non-TTY : Angular doit exécuter une seule passe et terminer ;
- test applicatif en échec : le job doit rester rouge ;
- build frontend en échec après tests verts : le job doit rester rouge.

## Hypothèses

- GitHub Actions utilise Ubuntu.
- `backend/mvnw` est le wrapper officiel du projet.
- le script `npm test` reste mappé vers `ng test`.
- Angular 22/Vitest désactive le watch par défaut en environnement non-TTY.