# Alignement BigDecimal des fixtures de tests financiers

## Objectif

Maintenir les tests backend alignés avec les contrats financiers `BigDecimal` sans réintroduire de `double` dans les entités et DTO migrés.

## Règles

- Les montants et coefficients attendus par un constructeur `BigDecimal` sont créés depuis une chaîne.
- Les valeurs conservent une échelle explicite de quatre décimales pour les montants et coefficients financiers concernés.
- Les changements restent limités aux données de préparation des tests.
- Aucun calcul métier, statut, endpoint, rôle ou schéma n’est modifié.

## Fichiers concernés

- `EstimateControllerTest`
- `FullFinancialE2ETest`
- `InsuranceBordereauControllerTest`
- `InvoiceControllerTest`
- `ReceivableReminderControllerTest`
- `CashRegisterControllerTest`

## Validation

La correction est validée uniquement si la compilation des tests et la suite Maven complète réussissent. Tout échec d’exécution restant doit être analysé comme un défaut distinct, sans désactiver de test.
