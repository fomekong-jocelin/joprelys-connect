# Conception technique — File d’encaissement caissier

## 1. Architecture

Le flux s’appuie sur le contrat financier livré par STORY-2201 et sur le workspace livré par STORY-2202.

### Backend

- `CashierCollectionQueueService` construit la file depuis les factures du tenant courant.
- `InvoiceFinancialStateService` reste l’unique source du `collectionStatus` et du reste patient.
- `PatientRepository` enrichit la réponse avec l’identité minimale du patient.
- `InvoiceController` expose un endpoint de lecture dédié.

### Frontend

- `BillingCashierQueueComponent` orchestre le chargement, la recherche, le filtre, l’encaissement et le reçu.
- `BillingCashRegisterComponent` conserve la gestion de session, les mouvements et la clôture, et héberge la file lorsque la session est active.
- `BillingPaymentModalComponent` est réutilisé avec un contexte minimal de facture et le reste patient comme maximum.

## 2. Endpoint

### GET `/api/invoices/collection-queue`

Rôles autorisés :

- `CAISSIER` ;
- `AGENT_ACCUEIL` ;
- `ADMIN_CLINIQUE` ;
- `DAF`.

Réponse : tableau trié par date de validation croissante, puis date de création croissante.

```json
[
  {
    "invoiceId": "uuid",
    "invoiceNumber": "FAC-20260710-000123",
    "patientId": "uuid",
    "patientName": "Nom du patient",
    "globalPatientNumber": "DPU-...",
    "phone": "+237...",
    "totalAmount": 100000.0000,
    "patientRemainingAmount": 20000.0000,
    "collectionStatus": "PATIENT_DUE",
    "createdAt": "2026-07-10T08:00:00Z",
    "validatedAt": "2026-07-10T08:15:00Z"
  }
]
```

## 3. Sélection backend

La requête initiale limite les candidats aux statuts persistants :

- `VALIDATED` ;
- `PARTIALLY_PAID`.

Chaque candidat est ensuite évalué par `InvoiceFinancialStateService.summarize(invoice)`. Seuls les statuts suivants sont conservés :

- `PATIENT_DUE` ;
- `PATIENT_PARTIALLY_PAID` ;

avec `patient.remainingAmount > 0.01`.

Cette double barrière empêche l’exposition d’une facture assurance seule, soldée, annulée ou non validée, y compris en présence de données historiques.

## 4. Isolation tenant

`InvoiceEntity` et `PatientEntity` portent `@TenantId`. Toutes les lectures s’exécutent dans le tenant Hibernate installé par le filtre JWT. Aucun identifiant d’organisation n’est accepté dans la requête HTTP.

Les tests couvrent explicitement l’absence de données d’une autre organisation.

## 5. DTO backend

`CashierCollectionQueueItemResponse` est un record immuable contenant uniquement les données nécessaires au poste caisse. Il ne renvoie ni détail médical, ni ligne de facture, ni part assurance détaillée.

## 6. Contrats Angular

Ajout de :

```ts
export interface CashierCollectionQueueItem {
  invoiceId: string;
  invoiceNumber: string;
  patientId: string;
  patientName: string;
  globalPatientNumber: string;
  phone?: string;
  totalAmount: number;
  patientRemainingAmount: number;
  collectionStatus: 'PATIENT_DUE' | 'PATIENT_PARTIALLY_PAID';
  createdAt: string;
  validatedAt?: string;
}
```

`BillingApiService.listCashierCollectionQueue()` appelle le nouvel endpoint.

## 7. Encaissement

Le composant réutilise :

- `POST /api/invoices/{invoiceId}/payments` ;
- `GET /api/cash-registers/payments/{paymentId}/receipt`.

Le backend existant reste responsable de :

- la session active ;
- la limite du montant ;
- la transaction ;
- la mise à jour de la créance et du statut facture ;
- la création du mouvement de caisse et du reçu.

Après succès, le frontend charge le reçu puis recharge la file depuis le serveur.

## 8. Composants UI

### `BillingCashierQueueComponent`

État interne :

- `items`, `loading`, `error` ;
- `searchQuery`, `statusFilter` ;
- `selectedItem`, `savingPayment` ;
- `receipt`, `receiptLoading`.

Valeurs calculées :

- éléments filtrés ;
- total restant dans la file ;
- nombre de dossiers en paiement partiel.

### Modale paiement

`BillingPaymentModalComponent` accepte un contrat minimal `BillingPaymentInvoiceContext` afin d’être réutilisable depuis le workspace facture et la file caisse. Le montant maximum correspond au reste patient fourni par la file.

## 9. Gestion des erreurs

- échec de chargement : état d’erreur local et retry ;
- échec de paiement : message local sans retirer l’entrée ;
- échec de chargement du reçu : paiement conservé, message informatif et file rechargée ;
- double clic : bouton désactivé pendant la requête ;
- réponse 204 de session active : session considérée fermée.

## 10. Performance

Le premier incrément retourne la file du tenant sans pagination. La sélection repository limite déjà les candidats aux factures validées ou partiellement payées. Une pagination ou un calcul batch des créances sera ajouté si les mesures pilote montrent une volumétrie importante.

## 11. Sécurité

- endpoint de file en lecture seule ;
- RBAC plus restrictif que la liste patient ;
- aucun paramètre tenant fourni par le client ;
- aucune donnée clinique dans le DTO ;
- paiement toujours exécuté par le service existant ;
- aucune règle financière dupliquée dans Angular.

## 12. Compatibilité

- aucune migration de base ;
- endpoint additif ;
- contrats existants inchangés ;
- impact SemVer MINOR.