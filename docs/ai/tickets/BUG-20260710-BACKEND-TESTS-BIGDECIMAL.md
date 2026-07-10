# BUG-20260710-BACKEND-TESTS-BIGDECIMAL — Finalisation de l’alignement BigDecimal

## Mode

Engineering / QA — correction P0 de la migration financière et de ses tests.

## Statut

DONE — correctif fusionné dans `main` via la PR #10 et validation combinée entièrement verte sur la PR temporaire #12.

## Problème

La migration des montants financiers de `Double` vers `BigDecimal` avait laissé deux incohérences :

1. dix-neuf constructions de tests utilisaient encore des littéraux `double` alors que les contrats ciblés attendaient désormais `BigDecimal` ;
2. `InvoiceItemEntity` déclarait des colonnes `NUMERIC(19,4)` mais conservait parfois une échelle 0 dans les objets non persistés, produisant par exemple `3` au lieu d’une valeur normalisée à quatre décimales.

## Cause

Les contrats de construction et le schéma ont évolué sans migration complète des fixtures ni normalisation centrale des valeurs reçues par l’entité.

## Périmètre

### Tests

- `EstimateControllerTest`
- `FullFinancialE2ETest`
- `InsuranceBordereauControllerTest`
- `InvoiceControllerTest`
- `ReceivableReminderControllerTest`
- `CashRegisterControllerTest`

### Production

- `InvoiceItemEntity` : normalisation de `unitPrice`, `quantity` et `coefficient` à l’échelle 4 avec `RoundingMode.HALF_UP`.

## Hors périmètre

- aucune modification d’endpoint ;
- aucune nouvelle règle financière ;
- aucune modification d’autorisation ou de multi-tenant ;
- aucune modification de migration Flyway dans cette PR ;
- les DTO financiers imbriqués encore en `Double` restent une dette séparée.

## Critères d’acceptation

- [x] Les 19 incompatibilités de type sont remplacées par des `BigDecimal` explicites.
- [x] Les assertions financières comparent des `BigDecimal` cohérents.
- [x] Les valeurs d’`InvoiceItemEntity` sont normalisées à quatre décimales avant calcul et exposition API.
- [x] Les valeurs financières et les règles de calcul restent inchangées.
- [x] `./mvnw clean verify -B -Dspring.profiles.active=test` réussit.
- [x] Les 276 tests backend réussissent.
- [x] Les tests Angular et le build de production réussissent.
- [x] Aucun test n’est désactivé, supprimé ou assoupli.

## Règle d’implémentation

Les valeurs financières de test sont créées depuis une chaîne :

```java
new BigDecimal("25000.0000")
```

L’entité centralise l’échelle financière afin d’éviter une représentation différente entre un objet pré-calculé et un objet rechargé depuis la base.

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

## Résultats de validation

Validation combinée sur la PR temporaire #12, avec le correctif CI et la migration V55 portable :

- Backend Maven strict : ✅
- Tests backend : ✅ 276 tests
- Flyway sur H2 : ✅
- Tests Angular : ✅
- Build Angular production : ✅

## Risques résiduels

- certains DTO de bordereaux utilisent encore `Double` ; ils ne sont pas modifiés ici pour préserver le périmètre ;
- la migration V55/H2 est corrigée et fusionnée via `BUG-20260710-V55-H2-COMPATIBILITY` ;
- la validation PostgreSQL 16 est automatisée avec Testcontainers ; la vérification de `flyway_schema_history` reste une précondition de déploiement partagé.

## Impact version

**PATCH** — finalisation rétrocompatible de la précision financière.

## Checklist

- [x] Ticket créé et mis à jour.
- [x] Périmètre limité à l’alignement BigDecimal.
- [x] Aucun contrat HTTP modifié.
- [x] CI combinée verte.
- [x] Résultats documentés.
- [x] Suivi projet et changelog finalisés après fusion.
