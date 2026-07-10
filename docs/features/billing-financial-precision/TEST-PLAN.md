# Billing Financial Precision — Test Plan

## Objectif

Prouver que la migration V55 s’exécute sur la base H2 des tests, conserve la précision financière attendue et ne provoque aucune régression backend.

## Vérifications automatiques obligatoires

### Suite backend complète

```bash
cd backend
./mvnw test -Dspring.profiles.active=test
./mvnw clean verify -Dspring.profiles.active=test
```

Résultat attendu :

- démarrage Flyway sans erreur sur V55 ;
- démarrage des contextes Spring ;
- tests unitaires et d’intégration réussis ;
- aucun test ignoré pour contourner la migration.

### CI GitHub

Workflow attendu : `.github/workflows/ci.yml`.

Job bloquant : `Backend — Maven Build & Tests`.

Commande CI :

```bash
./mvnw clean verify -B -Dspring.profiles.active=test
```

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

## Cas de données à couvrir

1. montant entier : `1000` → `1000.0000` ;
2. montant avec décimales : `1250.375` → valeur équivalente à l’échelle 4 ;
3. remise nulle → `0.0000` ;
4. coefficient nul → `1.0000` ;
5. taux d’assurance `0.8000` conservé ;
6. montant dépassant la précision cible → échec explicite, aucun tronquage silencieux.

## Non-régression

- création de facture ;
- précalcul de facture ;
- paiement ;
- créance patient/assurance ;
- bordereau d’assurance ;
- export comptable ;
- clôture de caisse.

## Vérification PostgreSQL

La syntaxe `ALTER COLUMN ... SET DATA TYPE` sans `USING` repose sur la conversion d’affectation PostgreSQL. Avant déploiement sur une base ayant déjà appliqué V55, vérifier l’état de `flyway_schema_history` : une migration versionnée déjà appliquée ne doit pas être réécrite en production.

## Critères de sortie

- CI backend verte ;
- aucun changement frontend requis ;
- diff limité à la migration et à la documentation ;
- validation du risque de migration déjà appliquée ;
- ticket et PR mis à jour avec les résultats.