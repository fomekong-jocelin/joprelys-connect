# 2026-07-24 — RECETTE reset / Flyway / `btree_gist`

## Contexte

Après fusion de HOS-STAFF-001-A / PR #140 dans `main` au commit `07db2d865afcdbf4767861431d8488af67e532b3`, la base RECETTE a été volontairement réinitialisée afin de repartir sans données legacy pendant la phase de développement.

Le reset a supprimé le schéma `public` avec `CASCADE`, y compris l'extension PostgreSQL `btree_gist`.

## Symptôme

- compilation backend : SUCCESS ;
- compilation frontend : SUCCESS ;
- démarrage backend : FAILURE ;
- healthcheck : `HTTP=000` sur `127.0.0.1:8085` ;
- erreurs Spring visibles : `organizationApiKeyRepository`, `jpaSharedEM_entityManagerFactory`, `entityManagerFactory`.

## Cause racine

Flyway a échoué dans une migration PostgreSQL utilisant `btree_gist` :

```text
ERROR: permission denied to create extension "btree_gist"
Hint: Must have CREATE privilege on current database to create this extension.
```

Le compte applicatif ne possède volontairement pas le privilège de création d'extension au niveau base.

Les erreurs JPA étaient des conséquences de l'échec Flyway et non la cause initiale.

## Correction opérationnelle validée

Après un reset complet de DEV/RECETTE :

```bash
sudo -u postgres psql -d joprelys_recette -c \
"CREATE EXTENSION IF NOT EXISTS btree_gist WITH SCHEMA public;"
```

Puis vérification :

```bash
sudo -u postgres psql -d joprelys_recette -c \
"SELECT extname, extversion FROM pg_extension WHERE extname = 'btree_gist';"
```

Le déploiement peut ensuite relancer Flyway et le backend normalement.

## Rollback — cas base vierge

Le rollback a également montré qu'un dump pris après reset peut ne pas contenir `flyway_schema_history`.

Le contrôle rollback doit donc tester l'existence de la table avant lecture :

```sql
SELECT CASE
    WHEN to_regclass('public.flyway_schema_history') IS NULL
        THEN 'FLYWAY_HISTORY_ABSENT'
    ELSE 'FLYWAY_HISTORY_PRESENT'
END;
```

## Décisions

- ne pas donner `SUPERUSER` ou des privilèges élevés permanents au compte applicatif ;
- installer les extensions PostgreSQL techniques avec un compte administrateur ;
- conserver les migrations Flyway déjà fusionnées immuables ;
- DEV/RECETTE restent réinitialisables tant que leurs données sont explicitement jetables ;
- après gel fonctionnel / données réelles, passer à des migrations non destructives avec stratégie de reprise ;
- le script de déploiement serveur doit à terme intégrer un preflight `btree_gist` et un contrôle `flyway_schema_history` tolérant une base vierge.

## Warnings non bloquants observés

- appels dépréciés `HospitalizationEntity.getRoomNumber()` dans `PdfGeneratorService` ;
- `DatePipe` Angular importé mais non utilisé dans un composant urgence ;
- budgets Angular légèrement dépassés sur le bundle initial et plusieurs CSS.

Ces warnings ne sont pas la cause de l'échec de démarrage et doivent être traités séparément.

## Documentation associée

Voir : `docs/operations/RECETTE-RESET-FLYWAY-RUNBOOK.md`.
