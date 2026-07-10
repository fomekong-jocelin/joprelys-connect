# Billing Financial Precision — Technical Design

## Contexte technique

Le backend Spring Boot utilise Flyway avec PostgreSQL en production et H2 pour les tests. La migration V55 actuelle contient :

```sql
ALTER COLUMN ... TYPE NUMERIC(...) USING value::NUMERIC(...)
```

Cette construction est spécifique à PostgreSQL et bloque H2. La CI exécute `./mvnw clean verify -Dspring.profiles.active=test` avec une datasource H2.

## Décision

Conserver une migration unique et portable en deux opérations explicites :

1. normaliser les valeurs nulles uniquement pour les colonnes dont la migration existante utilisait déjà `COALESCE` ;
2. modifier chaque colonne séparément avec la forme SQL standard `ALTER COLUMN ... SET DATA TYPE NUMERIC(...)` sans clause `USING`.

## Justification

- PostgreSQL accepte `ALTER COLUMN ... SET DATA TYPE` et applique la conversion d’affectation lorsque `USING` est omis.
- H2 supporte la modification de type avec `SET DATA TYPE` sans cast PostgreSQL.
- Une instruction par colonne réduit les écarts de parsing entre moteurs.
- Les `UPDATE` explicites préservent la sémantique des deux `COALESCE` de la migration initiale.
- Aucun code Java, contrat API ou calcul métier n’a besoin d’être modifié.

## Schéma cible

| Table | Colonne | Type cible |
|---|---|---|
| `invoices` | `total_amount` | `NUMERIC(19,4)` |
| `invoices` | `patient_share` | `NUMERIC(19,4)` |
| `invoices` | `insurance_share` | `NUMERIC(19,4)` |
| `invoices` | `discount_amount` | `NUMERIC(19,4)` |
| `invoice_items` | `unit_price` | `NUMERIC(19,4)` |
| `invoice_items` | `quantity` | `NUMERIC(19,4)` |
| `invoice_items` | `coefficient` | `NUMERIC(19,4)` |
| `invoice_items` | `total_item_amount` | `NUMERIC(19,4)` |
| `payments` | `amount` | `NUMERIC(19,4)` |
| `receivables` | `total_amount` | `NUMERIC(19,4)` |
| `receivables` | `paid_amount` | `NUMERIC(19,4)` |
| `tariff_grid` | `unit_value` | `NUMERIC(19,4)` |
| `insurance_conventions` | `coverage_percentage` | `NUMERIC(5,4)` |

## Normalisation pré-conversion

```sql
UPDATE invoices
SET discount_amount = 0
WHERE discount_amount IS NULL;

UPDATE invoice_items
SET coefficient = 1
WHERE coefficient IS NULL;
```

Aucune autre colonne ne sera normalisée afin d’éviter d’introduire une nouvelle règle métier.

## Impacts

### Backend

- Aucun changement Java.
- Aucun changement des entités ou DTO.
- Flyway peut terminer le démarrage des tests.

### Base de données

- Conversion de type uniquement.
- Arrondi possible à quatre décimales, identique à l’intention de V55.
- Échec explicite si une valeur dépasse la précision cible.

### Frontend

- Aucun impact.

### Sécurité et multi-tenant

- Aucun changement d’accès ou de données inter-organisations.
- La migration agit sur le schéma global comme les migrations précédentes.

## Stratégie de tests

1. Exécuter la suite Maven avec le profil `test`.
2. Vérifier que Flyway applique V55 sur H2.
3. Vérifier les tests financiers existants.
4. Vérifier le build complet `clean verify`.
5. Contrôler la CI de la Pull Request.

## Risques et mesures

| Risque | Mesure |
|---|---|
| Syntaxe différente entre H2 et PostgreSQL | Utiliser `SET DATA TYPE`, documenté par PostgreSQL et standard SQL pris en charge par H2 |
| Perte de la normalisation `COALESCE` | `UPDATE` explicites avant conversion |
| Arrondi des valeurs historiques | Échelle 4 inchangée par rapport à la migration initiale |
| Migration déjà appliquée ailleurs | Ne pas fusionner avant confirmation des environnements ; si appliquée en production, créer une migration corrective au lieu de réécrire V55 |
| Tests H2 insuffisants pour PostgreSQL | Planifier Testcontainers séparément ; ne pas élargir ce correctif P0 |

## Impact SemVer

PATCH — correction de compatibilité d’une migration existante, sans changement fonctionnel ni contrat public.

## Rollback

Le rollback automatique d’une conversion de type n’est pas recommandé. En cas d’échec avant livraison :

- restaurer la base depuis la sauvegarde de l’environnement concerné ;
- revenir au commit précédent ;
- ne jamais reconvertir les colonnes vers FLOAT8 en production sans validation DAF et DBA.