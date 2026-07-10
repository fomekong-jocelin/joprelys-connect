# BUG-20260710-BACKEND-TESTS-BIGDECIMAL — Tests backend non alignés avec BigDecimal

## Mode

Engineering / QA — correction P0 de compilation des tests financiers.

## Statut

IN_PROGRESS — compilation à valider par la CI empilée sur le correctif du pipeline.

## Problème

La migration des montants financiers de `Double` vers `BigDecimal` a été appliquée aux principales entités, DTO et services backend, mais plusieurs fixtures de tests construisent encore ces objets avec des littéraux `double`.

La compilation Maven des tests échoue avec 19 erreurs `double cannot be converted to java.math.BigDecimal` réparties dans six classes.

## Cause

Les contrats de construction ont évolué, mais les tests associés n’ont pas été migrés dans le même changement.

## Périmètre

- `EstimateControllerTest`
- `FullFinancialE2ETest`
- `InsuranceBordereauControllerTest`
- `InvoiceControllerTest`
- `ReceivableReminderControllerTest`
- `CashRegisterControllerTest`

## Hors périmètre

- aucune modification de service métier ;
- aucune modification d’endpoint ;
- aucune modification de migration Flyway ;
- aucune évolution des DTO financiers encore restés en `Double` ;
- aucun changement d’autorisation ou de multi-tenant.

## Critères d’acceptation

- [x] Les 19 incompatibilités de type identifiées sont remplacées par des `BigDecimal` explicites.
- [x] Les valeurs financières et leurs intentions restent inchangées.
- [x] Les échelles monétaires utilisent quatre décimales dans les fixtures concernées.
- [ ] `./mvnw clean verify -B -Dspring.profiles.active=test` compile tous les tests.
- [ ] La suite backend complète s’exécute.
- [ ] Aucun test n’est désactivé, supprimé ou assoupli.

## Règle d’implémentation

Utiliser des valeurs construites depuis une chaîne, par exemple :

```java
new BigDecimal("25000.0000")
```

Cela évite d’introduire une approximation binaire dans les données financières de test.

## Estimation

| Champ | Valeur |
|---|---|
| Priorité | P0 |
| Story points | 2 |
| Estimation senior | 0,3 j |
| Estimation intermédiaire | 0,5 j |
| Estimation junior | 0,8 j |
| Profil recommandé | Backend Java intermédiaire / senior |
| Reviewer | Lead Backend + QA finance |

## Tests attendus

- compilation des sources de test ;
- suite Maven complète ;
- tests E2E financiers ;
- vérification des assertions monétaires si la compilation révèle des différences de type à l’exécution.

## Risques

- assertions historiques comparant encore un `Double` attendu à un `BigDecimal` réel ;
- DTO financiers imbriqués restant en `Double`, à traiter dans un ticket séparé pour éviter d’élargir le correctif ;
- migration V55/H2 traitée séparément dans `BUG-20260710-V55-H2-COMPATIBILITY`.

## Impact version

Aucun bump applicatif : correction de tests uniquement.

## Checklist

- [x] Ticket créé.
- [x] Périmètre limité aux tests.
- [x] Aucun code de production modifié.
- [ ] CI backend verte.
- [ ] Résultats documentés.
- [ ] Suivi projet et changelog mis à jour si nécessaire.
