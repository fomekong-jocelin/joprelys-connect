# API-CONTRACT — Hospitalisation, Facturation et Caisse complètes

## 1. Vue d'ensemble

Ce contrat cible les endpoints à ajouter ou enrichir pour couvrir le séjour hospitalier complet, la facturation avancée, la caisse et les écritures OHADA minimales. Les endpoints existants `/api/hospitalizations`, `/api/invoices` et `/api/invoices/{id}/payments` restent compatibles.

## 2. Endpoints

### `POST /api/hospitalizations/{id}/daily-care`

#### Description

Ajoute une ligne structurée de feuille de soins journalière.

#### Auth / permissions

`INFIRMIER`, `MEDECIN`, `ADMIN_CLINIQUE`.

#### Request

```json
{
  "careDate": "2026-07-08",
  "careType": "DRESSING",
  "description": "Pansement post-opératoire",
  "vitalSigns": {
    "temperature": 37.2,
    "bloodPressure": "120/80",
    "pulse": 78
  },
  "billable": true
}
```

#### Response succès

```json
{
  "id": "uuid",
  "hospitalizationId": "uuid",
  "careType": "DRESSING",
  "billable": true,
  "createdAt": "2026-07-08T10:15:00Z"
}
```

#### Erreurs

| Code HTTP | Code fonctionnel | Cause | Message utilisateur |
|---:|---|---|---|
| 400 | DAILY_CARE_INVALID | Donnée invalide | La feuille de soins est incomplète. |
| 403 | FORBIDDEN | Rôle insuffisant | Accès refusé. |
| 404 | HOSPITALIZATION_NOT_FOUND | Séjour introuvable | Hospitalisation introuvable. |
| 409 | HOSPITALIZATION_CLOSED | Séjour clôturé | Impossible de modifier un séjour clôturé. |

### `POST /api/operating-room/reports`

#### Description

Crée ou valide un compte rendu opératoire et ses actes facturables.

#### Auth / permissions

`MEDECIN`, `ADMIN_CLINIQUE`, rôle opératoire à préciser.

#### Request

```json
{
  "hospitalizationId": "uuid",
  "interventionDate": "2026-07-08T08:00:00Z",
  "procedureName": "Ostéosynthèse",
  "anesthesiaType": "GENERAL",
  "surgeonUserId": "uuid",
  "anesthetistUserId": "uuid",
  "kSurgeon": 80,
  "kAnesthetist": 30,
  "kBloc": 25,
  "operativeFindings": "Constatations opératoires",
  "procedureSteps": "Description détaillée",
  "complications": "Aucune",
  "implants": [
    {
      "label": "Plaque verrouillée",
      "lotNumber": "LOT-001",
      "quantity": 1
    }
  ]
}
```

#### Response succès

```json
{
  "id": "uuid",
  "status": "VALIDATED",
  "billableItemsCreated": 3,
  "documentId": "uuid"
}
```

### `POST /api/billing/estimates`

#### Description

Crée un devis/proforma avant facture.

#### Auth / permissions

`SECRETAIRE_COMPTABLE`, `DAF`, `ADMIN_CLINIQUE`.

#### Request

```json
{
  "patientId": "uuid",
  "visitId": "uuid",
  "insuranceConventionId": "uuid",
  "items": [
    {
      "label": "Séjour standard",
      "itemType": "STAY_FEE",
      "unitPrice": 10000,
      "quantity": 3,
      "coefficient": null
    }
  ]
}
```

#### Response succès

```json
{
  "id": "uuid",
  "estimateNumber": "DEV-20260708-000001",
  "status": "DRAFT",
  "totalAmount": 30000,
  "patientShare": 30000,
  "insuranceShare": 0
}
```

### `POST /api/invoices/{id}/validate`

#### Description

Valide une facture et la rend immuable.

#### Auth / permissions

`SECRETAIRE_COMPTABLE`, `DAF`, `ADMIN_CLINIQUE`.

#### Request

```json
{
  "validationNote": "Contrôle terminé"
}
```

#### Response succès

```json
{
  "id": "uuid",
  "invoiceNumber": "FAC-20260708-000001",
  "status": "VALIDATED",
  "validatedAt": "2026-07-08T10:30:00Z"
}
```

#### Erreurs

| Code HTTP | Code fonctionnel | Cause | Message utilisateur |
|---:|---|---|---|
| 409 | INVOICE_ALREADY_VALIDATED | Facture déjà validée | Cette facture est déjà validée. |
| 409 | INVOICE_EMPTY | Aucune ligne | La facture ne contient aucune ligne. |

### `POST /api/cash-registers/{cashRegisterId}/sessions/open`

#### Description

Ouvre une session de caisse pour un caissier.

#### Auth / permissions

`CAISSIER`, `SECRETAIRE_COMPTABLE`, `DAF`, `ADMIN_CLINIQUE`.

#### Request

```json
{
  "openingFloat": 50000,
  "openingNote": "Fond de caisse initial"
}
```

#### Response succès

```json
{
  "id": "uuid",
  "cashRegisterId": "uuid",
  "status": "OPEN",
  "openedAt": "2026-07-08T07:30:00Z",
  "openingFloat": 50000
}
```

### `POST /api/cash-sessions/{sessionId}/payments`

#### Description

Encaisse une facture dans une session de caisse ouverte et génère un reçu.

#### Auth / permissions

`CAISSIER`, `SECRETAIRE_COMPTABLE`, `DAF`, `ADMIN_CLINIQUE`.

#### Request

```json
{
  "invoiceId": "uuid",
  "amount": 25000,
  "method": "CASH",
  "reference": null
}
```

#### Response succès

```json
{
  "paymentId": "uuid",
  "receiptId": "uuid",
  "receiptNumber": "REC-20260708-000001",
  "amount": 25000,
  "invoiceStatus": "PARTIALLY_PAID"
}
```

### `POST /api/cash-sessions/{sessionId}/close`

#### Description

Clôture la session de caisse.

#### Auth / permissions

`CAISSIER` pour proposition, `DAF` ou `ADMIN_CLINIQUE` pour validation si écart au-dessus du seuil.

#### Request

```json
{
  "declaredCashAmount": 180000,
  "closingNote": "Écart justifié par avance caisse validée",
  "dafApprovalUserId": "uuid",
  "medicalChiefApprovalUserId": "uuid"
}
```

#### Response succès

```json
{
  "id": "uuid",
  "status": "CLOSED",
  "theoreticalAmount": 180000,
  "declaredCashAmount": 180000,
  "differenceAmount": 0,
  "closedAt": "2026-07-08T18:00:00Z"
}
```

### `GET /api/receivables`

#### Description

Liste les créances patient et assurance.

#### Auth / permissions

`SECRETAIRE_COMPTABLE`, `DAF`, `ADMIN_CLINIQUE`.

### `GET /api/accounting/entries`

#### Description

Liste les écritures comptables générées automatiquement.

#### Auth / permissions

`DAF`, `ADMIN_CLINIQUE`.

### `POST /api/billing/insurance-bordereaux`

#### Description

Génère un bordereau récapitulatif pour les factures tiers-payant validées d'une convention sur une période donnée.

#### Auth / permissions

`SECRETAIRE_COMPTABLE`, `DAF`, `ADMIN_CLINIQUE`.

#### Request

```json
{
  "insuranceConventionId": "uuid",
  "startDate": "2026-07-01",
  "endDate": "2026-07-31"
}
```

#### Response succès

```json
{
  "id": "uuid",
  "bordereauNumber": "BORD-20260709-000001",
  "insuranceConventionId": "uuid",
  "startDate": "2026-07-01",
  "endDate": "2026-07-31",
  "totalAmount": 1250000.0,
  "status": "DRAFT",
  "createdAt": "2026-07-09T09:00:00Z"
}
```

### `GET /api/billing/insurance-bordereaux`

#### Description

Liste les bordereaux d'assurance générés.

#### Auth / permissions

`SECRETAIRE_COMPTABLE`, `DAF`, `ADMIN_CLINIQUE`.

#### Response succès

```json
[
  {
    "id": "uuid",
    "bordereauNumber": "BORD-20260709-000001",
    "insuranceConventionId": "uuid",
    "startDate": "2026-07-01",
    "endDate": "2026-07-31",
    "totalAmount": 1250000.0,
    "status": "DRAFT",
    "createdAt": "2026-07-09T09:00:00Z"
  }
]
```

### `GET /api/billing/insurance-bordereaux/{id}`

#### Description

Retourne les détails d'un bordereau, y compris la liste des factures associées.

#### Auth / permissions

`SECRETAIRE_COMPTABLE`, `DAF`, `ADMIN_CLINIQUE`.

#### Response succès

```json
{
  "id": "uuid",
  "bordereauNumber": "BORD-20260709-000001",
  "insuranceConventionId": "uuid",
  "startDate": "2026-07-01",
  "endDate": "2026-07-31",
  "totalAmount": 1250000.0,
  "status": "DRAFT",
  "createdAt": "2026-07-09T09:00:00Z",
  "invoices": [
    {
      "id": "uuid",
      "invoiceNumber": "FAC-20260708-000001",
      "patientName": "Jean Patient",
      "totalAmount": 150000.0,
      "insuranceShare": 120000.0,
      "status": "VALIDATED"
    }
  ]
}
```

### `POST /api/billing/insurance-bordereaux/{id}/send`

#### Description

Marque le bordereau comme expédié physiquement à l'assurance.

#### Auth / permissions

`SECRETAIRE_COMPTABLE`, `DAF`, `ADMIN_CLINIQUE`.

#### Response succès

```json
{
  "id": "uuid",
  "status": "SENT"
}
```

### `POST /api/billing/insurance-bordereaux/{id}/pay`

#### Description

Enregistre le règlement global du bordereau par l'assurance, solder les parts d'assurance des factures associées.

#### Auth / permissions

`DAF`, `ADMIN_CLINIQUE`.

#### Request

```json
{
  "amount": 1250000.0,
  "referenceNumber": "VIREMENT-998822"
}
```

#### Response succès

```json
{
  "id": "uuid",
  "status": "PAID"
}
```

## 3. Règles de compatibilité

- [x] Aucun champ public existant supprimé sans version majeure.
- [x] Aucun renommage silencieux.
- [x] Nouveaux champs rétrocompatibles documentés.
- [x] Erreurs documentées.
- [x] Impact SemVer évalué : MINOR si implémenté sans rupture.

## 4. Historique

| Date | Auteur | Changement |
|---|---|---|
| 2026-07-08 | Codex | Création du contrat API cible |
| 2026-07-09 | Antigravity | Spécification et démarrage de la STORY-2107 (Bordereaux d'assurance) |
