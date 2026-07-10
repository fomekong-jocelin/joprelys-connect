# Billing Financial Precision — Test Plan

## Objectif

Prouver que la migration V55 s’exécute sur H2 et PostgreSQL réel, conserve la précision financière attendue et ne provoque aucune régression backend ou frontend.

## Vérifications automatiques obligatoires

### Suite backend complète

```bash
cd backend
./mvnw clean verify -B -Dspring.profiles.active=test
```

Résultat attendu :

- démarrage Flyway sans erreur sur H2 ;
- démarrage des contextes Spring ;
- tests unitaires, d’intégration et E2E réussis ;
- test PostgreSQL Testcontainers exécuté ;
- aucun test ignoré pour contourner la migration.

### PostgreSQL 16 avec Testcontainers

Test :

```text
backend/src/test/java/com/joprelys/backend/database/FlywayPostgresqlMigrationTest.java
```

Le test doit :

1. démarrer obligatoirement `postgres:16-alpine` ;
2. exécuter toutes les migrations Flyway ;
3. exécuter `flyway.validate()` ;
4. confirmer que la version courante est au moins V55 ;
5. contrôler les précisions et échelles dans `information_schema.columns`.

Le test ne doit pas utiliser `disabledWithoutDocker = true` : l’absence de Docker doit faire échouer la validation, pas la rendre silencieusement verte.

### CI GitHub

Workflow : `.github/workflows/ci.yml`.

Jobs bloquants :

- `Backend — Maven Build & Tests` ;
- `Frontend Angular — Build & Tests`.

## Vérifications de schéma

Après application de V55, vérifier :

| Table | Colonne | Type attendu |
|---|---|---|
| invoices | total_amount | NUMERIC(19,4) |
| invoices | patient_share | NUMERIC(19,4) |
| invoices | insurance_share | NUMERIC(19,4) |
| invoices | discount_amount | NUMERIC(19,4) |
| invoice_items | unit_price | NUMERIC(19,4) |
| invoice_items | quantity | NUMERIC(19,4) |
| invoice_items | coefficient | NUMERIC(19,4) |
| invoice_items | total_item_amount | NUMERIC(19,4) |
| payments | amount | NUMERIC(19,4) |
| receivables | total_amount | NUMERIC(19,4) |
| receivables | paid_amount | NUMERIC(19,4) |
| tariff_grid | unit_value | NUMERIC(19,4) |
| insurance_conventions | coverage_percentage | NUMERIC(5,4) |

## Cas de données

1. montant entier : `1000` → `1000.0000` ;
2. montant avec décimales : `1250.375` → valeur équivalente à l’échelle 4 ;
3. remise nulle → `0.0000` ;
4. coefficient nul → `1.0000` ;
5. taux d’assurance `0.8000` conservé ;
6. montant dépassant la précision cible → échec explicite, aucun tronquage silencieux.

## Non-régression

- création et précalcul de facture ;
- paiement et reçu ;
- créance patient/assurance ;
- bordereau d’assurance ;
- export comptable ;
- clôture de caisse ;
- tests et build Angular.

## Résultats — PR temporaire #12

- H2 + Flyway : ✅
- PostgreSQL 16 Testcontainers : ✅
- Toutes les migrations Flyway : ✅
- Contrôle des 13 colonnes V55 : ✅
- Maven `clean verify` : ✅
- Suite backend complète : ✅
- Tests Angular : ✅
- Build Angular production : ✅

## Vérification avant déploiement

Avant déploiement sur un environnement partagé, inspecter `flyway_schema_history`. Une migration V55 déjà appliquée avec une autre empreinte ne doit pas être réécrite directement : il faut définir une stratégie de réparation ou une nouvelle migration versionnée.

## Critères de sortie

- CI backend et frontend vertes ;
- validation H2 et PostgreSQL réelle ;
- aucun test ignoré ;
- diff limité à V55, au test PostgreSQL et à la documentation ;
- risque d’une V55 déjà appliquée explicitement vérifié avant déploiement ;
- ticket et PR mis à jour avec les résultats.
