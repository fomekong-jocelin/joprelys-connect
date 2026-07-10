# STORY-2201 — Contrat fonctionnel d’état financier

## Objectif

Fournir un vocabulaire unique pour représenter le cycle de vie d’une facture et son niveau de recouvrement patient/assurance.

## Définitions métier

### Statut persistant de facture

| Statut | Définition |
|---|---|
| `PENDING` | Facture préparée mais non validée ; aucune créance exigible. |
| `PROFORMA` | Document informatif non exigible. |
| `VALIDATED` | Facture validée et créances initialisées ; part patient non réglée. |
| `PARTIALLY_PAID` | Une partie de la part patient a été encaissée. |
| `PAID` | La part patient est soldée, mais une part assurance reste à recouvrer. |
| `SETTLED` | Toutes les obligations patient et assurance sont soldées. |
| `CANCELLED` | Facture annulée ; aucune nouvelle opération de règlement autorisée. |

### État de recouvrement exposé aux écrans

| État | Définition |
|---|---|
| `NOT_YET_DUE` | Facture non validée ou sans créance exigible. |
| `PATIENT_DUE` | Part patient entièrement due. |
| `PATIENT_PARTIALLY_PAID` | Part patient partiellement réglée. |
| `INSURANCE_DUE` | Part patient soldée ; part assurance encore due. |
| `SETTLED` | Patient et assurance soldés. |
| `CANCELLED` | Facture annulée. |

## Invariants

1. Les créances constituent la source de vérité du recouvrement après validation.
2. Une facture validée possède au maximum une créance patient et une créance assurance.
3. Les créances manquantes sont créées de façon idempotente à partir des parts de la facture.
4. Un paiement patient ne peut cibler qu’une facture `VALIDATED` ou `PARTIALLY_PAID`.
5. Une facture `PAID`, `SETTLED`, `CANCELLED`, `PENDING` ou `PROFORMA` refuse un nouveau paiement patient.
6. Une facture sans part assurance devient `SETTLED` dès que la part patient est soldée.
7. Une facture tiers-payant devient `PAID` lorsque la part patient est soldée, puis `SETTLED` après règlement de l’assurance.
8. Si l’assurance est réglée avant le patient, le statut reste `VALIDATED` ou `PARTIALLY_PAID` selon le règlement patient.
9. La génération d’un bordereau ne prend que des factures validées ou en recouvrement ; une facture `PENDING` n’est jamais éligible.
10. Une facture annulée reste terminale.

## Scénarios d’acceptation

### Patient sans assurance

`PENDING → VALIDATED → PARTIALLY_PAID → SETTLED`

### Tiers-payant nominal

`PENDING → VALIDATED → PARTIALLY_PAID → PAID → SETTLED`

### Assurance à 100 %

Après validation, la part patient nulle est considérée soldée :

`PENDING → PAID → SETTLED`

### Assurance réglée avant le patient

`VALIDATED → règlement assurance → VALIDATED → règlement patient partiel → PARTIALLY_PAID → règlement patient complet → SETTLED`

### Annulation

`PENDING → CANCELLED`, puis aucune opération financière autorisée.

## Hors périmètre

- paiement partiel d’un bordereau d’assurance ;
- refonte comptable ou plan OHADA ;
- modification des règles de calcul des parts ;
- migration de tous les DTO financiers `Double` vers `BigDecimal`.
