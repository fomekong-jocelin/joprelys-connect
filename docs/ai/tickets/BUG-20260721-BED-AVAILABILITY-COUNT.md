# BUG-20260721 — Compteur fiable des lits disponibles

## Mode d'intervention

Engineering + correction P0 de risque métier, rattachée à `EPIC-0027 / HOS-BED-002`.

## Statut

QA TECHNIQUE VERTE — implémentation et contrôles automatisés terminés le 2026-07-21 ; revue humaine et recette métier restent requises.

## Objectif

Empêcher l'écran d'occupation de présenter comme libres les lits en nettoyage ou en maintenance.

## Constat et preuve

`SpatialManagementPageComponent` calcule actuellement les lits libres avec `totalBedsCount - occupiedBedsCount`. Un lit `CLEANING` ou `MAINTENANCE` est donc compté comme libre alors que l'admission backend ne peut revendiquer qu'un lit au statut `FREE`.

## Périmètre inclus

- ajouter au contrat d'occupation un compteur calculé côté backend à partir des lits `FREE` uniquement ;
- typer et afficher ce compteur dans Angular ;
- ajouter des tests de non-régression backend et frontend ;
- mettre à jour la documentation, le backlog et le suivi.

## Périmètre exclu

- refonte complète des axes installé/ouvert/hygiène/usage ;
- réservation anticipée, contrainte temporelle PostgreSQL et nouvelle machine à états ;
- modification des permissions ou de l'endpoint générique de statut ;
- modification du taux d'occupation historique, dont le dénominateur « lits ouverts » n'existe pas encore.

## Règles métier

1. Un lit est compté disponible dans le modèle legacy uniquement si son statut est exactement `FREE`.
2. Les lits `OCCUPIED`, `CLEANING` et `MAINTENANCE` ne sont jamais comptés disponibles.
3. `totalBedsCount` reste le nombre de lits physiquement configurés.
4. `occupiedBedsCount` reste le nombre de lits au statut `OCCUPIED`.
5. Angular affiche le compteur fourni par le backend et ne recalcule pas la disponibilité.

## Critères d'acceptation

- Étant donné un service avec un lit libre, un lit occupé, un lit en nettoyage et un lit en maintenance, lorsque l'occupation est chargée, alors `availableBedsCount` vaut 1.
- Étant donné un lit libre passé en maintenance, lorsque l'occupation est rechargée, alors ce lit n'est plus compté disponible.
- Étant donné la réponse backend, lorsque l'écran affiche « lits libres », alors il utilise `availableBedsCount` sans soustraction locale.
- Étant donné les consommateurs actuels, lorsque le nouveau champ est ajouté, alors les champs historiques restent inchangés.

## Impacts et risques

- Backend/API : champ de réponse additif `availableBedsCount`.
- Angular : modèle `WardOccupancy` et compteur de la page spatiale.
- Base/configuration/Flutter/CI-CD : aucun changement.
- Régression principale : consommateurs stricts d'un JSON fermé ; considérée faible, le contrat est additif.

## Estimation et responsabilité

- Estimation : 2 SP, 1 jour senior incluant documentation et tests.
- Profil recommandé : senior full-stack.
- Reviewer : lead full-stack + cadre infirmier/bed manager.
- Sprint : incrément P0 autorisé, sans engagement du reste d'EPIC-0027.

## Actions

- [x] Reconstituer le calcul actuel et ses consommateurs.
- [x] Écrire la spécification fonctionnelle, la conception technique, le contrat API et le plan de test.
- [x] Ajouter le compteur backend fondé sur `BedStatus.FREE`.
- [x] Ajouter les tests backend couvrant nettoyage et maintenance.
- [x] Utiliser le compteur backend dans Angular.
- [x] Ajouter le test Angular de non-régression.
- [x] Exécuter les vérifications ciblées et disponibles.
- [x] Mettre à jour backlog, tracking, changelog et statut final.

## Tests / vérifications

- `mvn -Dtest=SpatialControllerTest test` : 10 tests réussis.
- `mvn clean verify` : 480 tests, 0 échec, 0 erreur, 1 test PostgreSQL/Testcontainers ignoré faute de Docker.
- `npm test -- --no-watch --include "src/app/clinic/spatial/spatial-management-page.component.spec.ts" --include "src/app/clinic/spatial/spatial-configuration-page.component.spec.ts"` : 5 tests réussis dans 2 fichiers.
- `$env:NODE_OPTIONS='--no-experimental-webstorage'; npm test -- --no-watch` : 315 tests réussis dans 67 fichiers.
- `npm run build` : build Angular de production réussi, avec budgets historiques encore dépassés.

Le contournement `--no-experimental-webstorage` neutralise l'implémentation expérimentale et invalide de `localStorage` de Node.js 25.9.0. Il ne modifie pas le code applicatif ; l'environnement CI doit rester sur une version Node LTS supportée.

## Checklist de review

- [x] Le backend reste maître de la règle de disponibilité.
- [x] Le contrat API est additif ; aucun champ historique n'est retiré ou renommé.
- [x] Aucun changement de base, configuration, permission ou donnée patient.
- [x] Aucun texte UI, token de thème ou règle i18n n'est ajouté.
- [x] Tests ciblés, suites complètes et build exécutés.
- [ ] Revue fonctionnelle du libellé et du compteur par un cadre infirmier ou bed manager.
- [ ] Recette sur un environnement déployé avec livraison backend avant frontend.

## Dette technique observée

`SpatialManagementPageComponent` atteint 327 lignes : il dépasse le seuil d'alerte de 300 lignes du standard SOLID, tout en restant sous la limite bloquante de 500. Une extraction des cartes de synthèse et du plan de lits devra être intégrée au découpage ultérieur de HOS-BED-002 ; elle n'est pas mêlée à ce correctif P0.

## Impact version / SemVer

Ajout d'un champ API rétrocompatible : cible probable `MINOR` (`0.11.0`) selon le standard du dépôt. Aucun bump ni release n'est préparé dans cette intervention.

## Reste à faire

Faire relire, livrer le backend avant le frontend, puis réaliser la recette métier. La refonte complète de capacité, les invariants transactionnels et les statuts installé/ouvert/fermé restent portés par HOS-BED-001 et HOS-BED-002.
