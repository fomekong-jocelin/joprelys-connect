# STORY-2201 — Contrat API des états financiers

## Facture

Le champ existant `status` conserve son nom et son format JSON chaîne.

Valeurs :

```text
PENDING
PROFORMA
VALIDATED
PARTIALLY_PAID
PAID
SETTLED
CANCELLED
```

Sémantique :

- `PAID` : part patient soldée, assurance encore due ;
- `SETTLED` : patient et assurance soldés.

## Synthèse de règlement

Endpoint inchangé :

```http
GET /api/invoices/settlement-summaries?patientId={patientId}
```

Réponse :

```json
{
  "invoiceId": "uuid",
  "collectionStatus": "INSURANCE_DUE",
  "patient": {
    "totalAmount": 20000.0000,
    "paidAmount": 20000.0000,
    "remainingAmount": 0.0000,
    "status": "PAID"
  },
  "insurance": {
    "totalAmount": 80000.0000,
    "paidAmount": 0.0000,
    "remainingAmount": 80000.0000,
    "status": "UNPAID"
  }
}
```

Valeurs de `collectionStatus` :

```text
NOT_YET_DUE
PATIENT_DUE
PATIENT_PARTIALLY_PAID
INSURANCE_DUE
SETTLED
CANCELLED
```

## Compatibilité

Les noms des champs et les valeurs historiques restent inchangés. Les seules extensions sont :

- `Invoice.status = SETTLED` ;
- `collectionStatus = CANCELLED`.

Les clients doivent traiter les enums de manière exhaustive et afficher un fallback pour toute valeur inconnue.
