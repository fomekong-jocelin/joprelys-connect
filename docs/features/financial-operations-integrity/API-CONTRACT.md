# Contrat API - Intégrité financière

## Paiement de facture

`POST /api/invoices/{invoiceId}/payments` conserve son contrat public. Dans la même transaction, il crée le paiement, le mouvement de caisse, le reçu et met à jour les créances de type `PATIENT` associées à la facture.

Les créances `INSURANCE` ne sont pas modifiées par cet endpoint. Elles sont soldées exclusivement lors du règlement d'un bordereau d'assurance.

## Liste des créances

`GET /api/receivables/by-status?status=ALL` retourne désormais toutes les créances du tenant, ordonnées par date décroissante. Les valeurs existantes `UNPAID`, `PARTIALLY_PAID` et `PAID` restent inchangées.

## Synthèse de règlement

`GET /api/invoices/settlement-summaries?patientId={uuid}` retourne, pour chaque facture du patient, les montants et états de règlement séparés pour le patient et l'assurance.

États de collection: `NOT_YET_DUE`, `PATIENT_DUE`, `PATIENT_PARTIALLY_PAID`, `INSURANCE_DUE`, `SETTLED`.

## Nouveaux Contrats API (Caisse, Recouvrement, DAF)

Les détails des contrats API associés aux opérations de caisse et de clôture, ainsi que le grand livre comptable OHADA et les exports, sont documentés dans [DAF-VALIDATION.md](file:///C:/MES-APPLICATIONS/joprelys-connect/docs/features/financial-operations-integrity/DAF-VALIDATION.md).

