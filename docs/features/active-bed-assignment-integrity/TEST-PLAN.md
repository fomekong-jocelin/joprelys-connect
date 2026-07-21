# Plan de test — Intégrité des affectations actives de lit

## INT-BED-001 — Refus d'un doublon actif

- Préconditions : un lit et une affectation avec `released_at = NULL`.
- Action : persister une seconde affectation active du même lit.
- Attendu : violation d'intégrité ; une seule ligne active demeure.

## INT-BED-002 — Réutilisation après clôture

- Préconditions : une affectation active.
- Action : renseigner `released_at`, puis créer une nouvelle affectation.
- Attendu : succès ; l'ancienne ligne reste historisée avec marqueur nul.

## INT-BED-003 — Marqueur incohérent

- Action : insérer directement une ligne active avec `active_bed_id = NULL` ou différent de `bed_id`.
- Attendu : refus par `ck_bed_assignments_active_bed_consistency`.

## INT-BED-004 — Traduction applicative

- Préconditions : le repository déclenche une violation lors du flush.
- Action : demander la création d'une affectation active.
- Attendu : HTTP 409 sans donnée concurrente exposée.

## INT-BED-005 — Migration H2

- Action : démarrer le contexte de test et exécuter toutes les migrations jusqu'à V76.
- Attendu : validation Hibernate et tests d'intégrité verts.

## INT-BED-006 — Migration PostgreSQL 16

- Action : exécuter `FlywayPostgresqlMigrationTest` avec Docker disponible.
- Attendu : V76 appliquée, colonne/contrainte/index présents, doublon actif refusé.

## INT-BED-007 — Non-régression

- Admission normale, transfert et libération restent fonctionnels.
- `mvn clean verify` doit rester vert.

## Contrôle de déploiement

Exécuter le préflight de `FUNCTIONAL-SPEC.md`, sauvegarder la base, appliquer backend/migration, puis vérifier le nombre d'affectations actives par lit.
