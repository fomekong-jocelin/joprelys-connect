# TECHNICAL-DESIGN — Workspace Factures orienté tâche

## Architecture

```text
BillingManagementPageComponent
  ├─ charge factures + synthèses de règlement
  ├─ porte les états loading/error/selectedInvoice
  ├─ ouvre le panneau de détail et orchestre le focus
  └─ BillingInvoiceHistoryComponent
       ├─ composant de présentation
       ├─ affiche les soldes fournis par InvoiceSettlementSummary
       └─ émet retry, selectInvoice, openPayment, openInsurance et printPdf
```

## Responsabilités

### Parent

- orchestrer les appels API avec `forkJoin` ;
- tolérer l’absence temporaire des synthèses sans masquer les factures ;
- distinguer chargement et erreur ;
- conserver l’identifiant sélectionné ;
- déplacer le focus vers le panneau après rendu ;
- restaurer le focus vers la carte correspondante à la fermeture.

### Historique

- rester sans accès API ;
- ne calculer aucun solde ;
- utiliser les montants du read model lorsqu’il existe ;
- appliquer un fallback de présentation prudent pour les données historiques sans synthèse ;
- exposer une méthode publique `focusInvoice(id)` réservée à la restauration du focus ;
- utiliser un template et un fichier CSS séparés.

## État de chargement

`loadPatientHistory(patientId)` combine :

- `listInvoices(patientId)` obligatoire ;
- `listInvoiceSettlementSummaries(patientId)` tolérant une erreur avec une collection vide.

Le chargement se termine via `finalize`. Une erreur sur les factures positionne l’état d’erreur et vide les données pour éviter l’affichage de valeurs obsolètes.

## Contrats composants

Nouveaux inputs de `BillingInvoiceHistoryComponent` :

- `loading: boolean` ;
- `error: boolean` ;
- `selectedInvoiceId: string | null`.

Nouvel output :

- `retry: EventEmitter<void>`.

Les outputs existants sont conservés ; aucune rupture pour le parent.

## Focus et clavier

- le panneau possède `tabindex="-1"` et une référence `ViewChild` ;
- `NgZone.onStable.pipe(take(1))` déclenche le focus après insertion DOM ;
- Échap ferme le panneau ;
- à la fermeture, l’historique recherche la carte par `data-invoice-id` et lui redonne le focus ;
- la carte sélectionnée porte `aria-current="true"`.

## Design

- rayon maximal 8 px via les tokens existants ;
- aucune ombre forte sur les cartes, uniquement le panneau latéral ;
- grille financière responsive ;
- badge uniquement pour le statut réel ;
- action financière primaire, actions de consultation secondaires ;
- styles light/dark basés sur les variables CSS du projet.

## Sécurité et données

- aucune modification RBAC ou API ;
- aucun calcul financier ajouté dans Angular ;
- aucun stockage de donnée sensible ;
- la logique de visibilité des paiements reste fondée sur le contrat STORY-2201.

## Impact

Frontend uniquement, sans migration ni changement backend. Impact SemVer MINOR pour l’amélioration fonctionnelle et accessible.