# Conception technique - Intégrité financière et postes métier

## Défaut diagnostiqué

`BillingService.addPayment` persiste le paiement et met à jour `InvoiceEntity.status`, sans appeler `ReceivableEntity.setPaidAmount` pour la créance `PATIENT`. Le suivi de créances reste donc à `UNPAID` après le règlement affiché dans l'historique facture.

## Correctif STORY-2111

Le cas d'usage de paiement met à jour, dans la même transaction, les créances de type `PATIENT` de la facture. Les créances assurance sont exclues de cette mise à jour et sont réglées uniquement par `InsuranceBordereauService.recordPayment`.

L'UI de l'historique facture doit distinguer explicitement: règlement patient complet, règlement patient partiel et part assurance encore due. Elle ne déduit pas cet état sans données de backend à terme; la première itération peut s'appuyer sur la convention et les créances de la facture, puis STORY-2112 expose un DTO de synthèse métier.

## Cible STORY-2112+

Créer un read model `InvoiceSettlementSummary` retournant les montants initiaux, réglés et restant dus par débiteur, l'état documentaire et les actions autorisées selon rôle. Les composants Angular deviennent des présentateurs de ce contrat; aucune règle de solde ne reste dans les templates.

## Implémentation STORY-2112

`InvoiceSettlementQueryService` expose un read model non persistant par patient. Il agrège les créances de chaque facture pour retourner les montants patient et assurance, ainsi que l'un des états: `NOT_YET_DUE`, `PATIENT_DUE`, `PATIENT_PARTIALLY_PAID`, `INSURANCE_DUE` ou `SETTLED`.

Le composant d'historique Angular reçoit ce contrat sous forme de map par identifiant de facture. Il ne décide plus qu'une facture est soldée à partir du champ technique `InvoiceStatus`.

## Tests

- Paiement patient complet/partiel synchronise les créances dans la transaction.
- Assurance réglée via bordereau ne modifie pas la créance patient.
- Toutes les listes de créances retournent des données tenant-aware et filtrées serveur.
- Les composants rendent les états métier à partir d'un DTO connu.

## Conception technique des modules caissier, recouvrement et DAF

Le détail de la conception technique du flux de caisse, de la balance âgée pour le recouvrement, et le schéma comptable OHADA sont décrits dans [DAF-VALIDATION.md](file:///C:/MES-APPLICATIONS/joprelys-connect/docs/features/financial-operations-integrity/DAF-VALIDATION.md).

