# STORY-2201 — Contrat d’état financier unique patient / assurance

## Mode

Diagnostic + Architecture + Engineering.

## Statut

IN_PROGRESS — implémentation terminée, validation finale Maven/Angular en cours.

## Objectif

Éliminer les contradictions entre le statut persistant d’une facture, les créances patient/assurance, les règlements et le read model exposé au frontend.

## Constat initial

- `InvoiceStatus.PAID` était posé lorsque la part patient était réglée, même si une part assurance restait à recouvrer.
- Les créances portaient un statut texte indépendant (`UNPAID`, `PARTIALLY_PAID`, `PAID`).
- Le read model recalculait un troisième état sous forme de chaîne libre.
- Le règlement d’un bordereau soldait les créances assurance sans synchroniser explicitement la facture.
- L’export comptable ignorait les factures totalement soldées dès l’introduction de `SETTLED`.

## Contrat retenu

- `PAID` : part patient soldée, assurance encore due ;
- `SETTLED` : toutes les obligations patient et assurance sont soldées ;
- une facture sans assurance devient directement `SETTLED` après paiement patient complet ;
- une facture non validée, annulée ou déjà soldée refuse tout nouvel encaissement ;
- les créances constituent la source de vérité du recouvrement après validation.

## Périmètre

- statuts et transitions des factures ;
- créances patient et assurance ;
- règlements patient ;
- génération, envoi et règlement des bordereaux ;
- read model `InvoiceSettlementSummaryResponse` ;
- export comptable Sage 100 ;
- contrats API et usages frontend ;
- tests unitaires, intégration et E2E.

## Plan d’action

- [x] Confirmer les divergences initiales entre facture, créances et read model.
- [x] Cartographier les lectures et écritures de statuts concernées.
- [x] Définir le vocabulaire métier cible et les invariants.
- [x] Documenter le contrat fonctionnel, technique et API.
- [x] Implémenter `InvoiceFinancialStateService` comme source unique de calcul/synchronisation.
- [x] Remplacer le statut de synthèse libre par `InvoiceCollectionStatus`.
- [x] Synchroniser validation, paiement patient, règlement assurance et avoirs.
- [x] Ajouter `InvoiceStatus.SETTLED` et adapter l’export Sage 100.
- [x] Protéger l’immuabilité : annulation et remise uniquement sur facture `PENDING`.
- [x] Étendre les contrats Angular et masquer l’encaissement sur facture soldée.
- [x] Couvrir patient seul, tiers payant, assurance avant patient, règlement complet et annulation.
- [ ] Valider la suite Maven complète, H2 et PostgreSQL 16.
- [x] Valider les tests Angular et le build de production.
- [ ] Mettre à jour le suivi central et le changelog après validation verte.

## Critères d’acceptation

- [x] Un même état métier n’est plus interprété différemment selon l’écran ou l’endpoint.
- [x] `PAID` et `SETTLED` ont des définitions non ambiguës et couvertes par tests.
- [x] Le règlement patient et le règlement assurance déclenchent une synchronisation déterministe.
- [x] Aucun paiement n’est accepté sur une facture non validée, annulée ou totalement soldée.
- [x] Les transitions restent atomiques et tenant-safe.
- [x] Les noms de champs HTTP existants sont conservés ; seules de nouvelles valeurs d’enum sont ajoutées.

## Estimation et réalisation

| Champ | Valeur |
|---|---|
| Priorité | P0 |
| Story points | 5 |
| Estimation senior | 1,5 j |
| Profil recommandé | Backend senior + DAF / Product |
| Reviewer | Lead Developer + DAF |
| Sprint | SPRINT-0014 |
| Temps passé | 1,2 j |

## Vérifications intermédiaires

- tests Angular : ✅ ;
- build Angular production : ✅ ;
- 280 tests backend déjà verts lors du diagnostic ;
- deux échecs historiques identifiés puis corrigés : validation préalable du test caisse et inclusion de `SETTLED` dans l’export Sage ;
- migration PostgreSQL 16 : ✅ lors de la passe de diagnostic.

## Risques restants

- données historiques sans créances : reconstruction prudente et idempotente, à surveiller lors du premier déploiement ;
- évolution sémantique de `PAID` à communiquer à la DAF/Product ;
- paiement partiel d’un bordereau assurance reste hors périmètre ;
- DTO financiers résiduels en `Double` restent une dette séparée.
