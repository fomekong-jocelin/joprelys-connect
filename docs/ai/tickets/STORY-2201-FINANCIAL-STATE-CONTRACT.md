# STORY-2201 — Contrat d’état financier unique patient / assurance

## Mode

Diagnostic + Architecture + Engineering.

## Statut

IN_PROGRESS — cartographie des statuts et transitions existants avant conception du contrat cible.

## Objectif

Éliminer les contradictions entre le statut persistant d’une facture, les créances patient/assurance, les règlements et le read model exposé au frontend.

## Constat initial

- `InvoiceStatus.PAID` est actuellement posé lorsque la part patient est réglée, même si une part assurance reste à recouvrer.
- Les créances portent un statut texte indépendant (`UNPAID`, `PARTIALLY_PAID`, `PAID`).
- Le read model de synthèse recalcule un troisième état sous forme de chaîne (`PATIENT_DUE`, `INSURANCE_DUE`, `SETTLED`).
- Le règlement d’un bordereau solde les créances assurance sans synchroniser explicitement le statut persistant de la facture.

## Périmètre du diagnostic

- statuts et transitions des factures ;
- créances patient et assurance ;
- règlements patient ;
- génération, envoi et règlement des bordereaux ;
- read model `InvoiceSettlementSummaryResponse` ;
- contrats API et usages frontend ;
- tests d’intégration et E2E existants.

## Plan d’action

- [x] Confirmer les divergences initiales entre facture, créances et read model.
- [ ] Cartographier toutes les lectures et écritures de statuts.
- [ ] Définir le vocabulaire métier cible et les invariants.
- [ ] Documenter le contrat fonctionnel et technique.
- [ ] Implémenter une source unique de calcul/synchronisation.
- [ ] Remplacer les chaînes libres par des types explicites lorsque compatible.
- [ ] Couvrir les scénarios patient seul, tiers payant, paiement partiel, règlement assurance et annulation.
- [ ] Valider l’isolation tenant et la non-régression RBAC.
- [ ] Mettre à jour le suivi central et le changelog.

## Critères d’acceptation

- un même état métier ne peut plus être interprété différemment selon l’écran ou l’endpoint ;
- `PAID` et `SETTLED` ont des définitions non ambiguës et testées ;
- le règlement patient et le règlement assurance déclenchent une synchronisation déterministe ;
- aucun paiement n’est accepté sur une facture annulée ou totalement soldée ;
- les transitions sont atomiques et tenant-safe ;
- les contrats exposés restent rétrocompatibles ou leur évolution est explicitement documentée.

## Estimation initiale

| Champ | Valeur |
|---|---|
| Priorité | P0 |
| Story points | 5 |
| Estimation senior | 1,5 j |
| Profil recommandé | Backend senior + DAF / Product |
| Reviewer | Lead Developer + DAF |
| Sprint | SPRINT-0014 |

## Risques

- modification sémantique de `PAID` visible par le frontend ;
- données historiques potentiellement incohérentes ;
- dépendances des bordereaux à la liste actuelle des statuts éligibles ;
- coexistence temporaire de statuts persistants et dérivés.
