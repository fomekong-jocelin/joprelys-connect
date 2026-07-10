# TECHNICAL-DESIGN — Workspace Facturation & Caisse orienté tâche

## Stack

- Angular 22 standalone, Signals, Tailwind CSS v4.
- Spring Boot / Maven pour les règles financières et les contrats REST existants.
- PostgreSQL/H2 via Flyway, sans migration prévue à la première itération.

## Architecture cible

```text
BillingWorkspacePage
  ├─ InvoiceWorkspace (création ou facture existante)
  ├─ InvoiceSettlementDrawer (paiement, reçu, créances)
  ├─ CashierWorkspace (session et file d'encaissement)
  └─ InsuranceCollectionsWorkspace (bordereaux)
```

Les composants de présentation ne recalculent pas les soldes. Ils consomment le read model de règlement exposé par le backend et délèguent les mutations aux services Angular.

## Contrats à stabiliser

- `InvoiceSettlementSummary` comme source de l'état affiché.
- Payment API : montant restant, session active, référence et reçu.
- Insurance bordereau API : statut, montant, factures associées et prochaine action.

## Impacts backend à auditer

- Vérifier que `Invoice.status` n'est plus présenté comme un statut global lorsque la part assurance reste ouverte.
- Vérifier le recalcul transactionnel des créances patient et assurance.
- Vérifier l'idempotence et les erreurs `409` sur paiement, bordereau et concurrence.
- Conserver les contrôles RBAC et le cloisonnement tenant.

## Impacts frontend

- Extraire les sections actuellement empilées dans des composants dédiés.
- Utiliser une vue secondaire/panneau pour le détail de facture.
- Remplacer les boutons longs par une action principale courte et un résumé de montant.
- Pour les sections devis/avoirs, distinguer les états de lecture des actions de mutation par des groupes visuels dédiés, des libellés persistants, des états ARIA et des tokens de couleur existants ; aucun contrat API n’est requis.
- La grille de saisie des lignes de devis utilise des colonnes flexibles (`minmax(0, …)`) et des container queries sur le formulaire, afin de s’adapter à la largeur réelle du panneau latéral plutôt qu’à la seule largeur de la fenêtre. L’action de suppression conserve un libellé i18n FR/EN.
- Préserver l'i18n FR/EN, le thème centralisé et les tokens de radius/ombres.
- Dans l'en-tête de la liste des bordereaux, neutraliser le `width: 100%` global de `.ui-select` à partir du breakpoint `sm` avec une largeur utilitaire bornée et non compressible ; conserver une pile verticale et `width: 100%` sous ce breakpoint. Le titre devient non compressible sur tablette/desktop afin d'éviter son retour à la ligne.
- Extraire le template et les styles de `BillingEstimatesComponent` afin de ramener le TypeScript sous la limite de 500 lignes.
- Réutiliser `ConfirmationDialogComponent` pour les confirmations destructives, avec focus initial, Échap, blocage pendant la requête et restauration du focus.
- Le parent transmet la liste des visites et la visite active au panneau devis ; le contrat `visitId?` existant est conservé.

## Tests

- Tests composants des états `PATIENT_DUE`, `PATIENT_PARTIALLY_PAID`, `INSURANCE_DUE`, `SETTLED`.
- Tests de focus après ouverture du détail.
- Tests de création de devis avec `visitId`, validation/annulation de facture, avoir et bannières de feedback.
- Tests de la modale : confirmation, Échap et état pending.
- Tests de non-émission sur visite déjà facturée.
- Tests backend paiements partiels/complets et règlement assurance.
- E2E : facture → caisse → paiement patient → bordereau → paiement assurance → `SETTLED`.

## SemVer

MINOR prévu, aucun changement cassant si les endpoints et DTO existants restent compatibles.
