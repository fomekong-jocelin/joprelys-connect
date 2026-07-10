# TASK-20260710-P0-BASELINE-CLOSURE — Clôture documentaire de la baseline P0

## Mode

QA Review / Project Tracking.

## Statut

DONE — les tickets CI, V55 et BigDecimal sont fusionnés, clôturés et reportés dans le suivi central et le changelog.

## Périmètre

- passage à `DONE` des tickets `BUG-20260710-CI-BASELINE-EXECUTION`, `BUG-20260710-V55-H2-COMPATIBILITY` et `BUG-20260710-BACKEND-TESTS-BIGDECIMAL` ;
- correction des références de validation vers la PR temporaire #12 ;
- mise à jour de `docs/ai/PROJECT-TRACKING.md` ;
- mise à jour de `docs/ai/CHANGELOG.md` ;
- aucune modification du code applicatif, du schéma ou du pipeline.

## Validation

- PR #9 fusionnée : CI Maven/Angular ;
- PR #8 fusionnée : V55 H2/PostgreSQL 16 ;
- PR #10 fusionnée : alignement BigDecimal ;
- dernière CI avant fusion : backend Maven, PostgreSQL Testcontainers, tests Angular et build de production verts.

## Risques restants

- vérifier `flyway_schema_history` avant déploiement sur un environnement partagé ;
- traiter dans un ticket séparé les DTO financiers résiduels en `Double` ;
- poursuivre `STORY-2201` pour arbitrer le modèle d’état financier `PAID` / `SETTLED`.

## Impact version

Aucun changement applicatif supplémentaire. La baseline fusionnée correspond à un impact **PATCH**.
