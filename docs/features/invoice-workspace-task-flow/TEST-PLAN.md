# TEST-PLAN — Workspace Factures orienté tâche

## Tests composants Angular

### `BillingInvoiceHistoryComponent`

- affiche un état de chargement et masque l’état vide ;
- affiche une erreur avec une action de nouvelle tentative ;
- affiche l’état vide uniquement lorsque le chargement est terminé sans erreur ;
- rend les restes patient/assurance issus de `InvoiceSettlementSummary` ;
- propose `Encaisser` pour `PATIENT_DUE` et `PATIENT_PARTIALLY_PAID` ;
- propose `Suivre l’assurance` pour `INSURANCE_DUE` ;
- ne propose aucune action financière pour `SETTLED` et `CANCELLED` ;
- marque la facture sélectionnée avec `aria-current` ;
- émet les événements détail, PDF, paiement, assurance et retry ;
- restaure le focus sur une carte via `focusInvoice(id)`.

### `BillingManagementPageComponent`

- transmet les états loading/error/selected à l’historique ;
- recharge l’historique après retry ;
- ouvre le panneau avec l’identifiant sélectionné ;
- place le focus dans le panneau après rendu ;
- ferme le panneau avec Échap ;
- restaure le focus sur la facture précédemment sélectionnée ;
- utilise `selectedInvoice.patientId` et `selectedInvoice.visitId` pour supporter les deep-links.

## Tests responsive et visuels

À vérifier sur 360 px, 768 px et 1440 px :

- aucun débordement horizontal ;
- boutons tactiles lisibles ;
- actions sur une ligne en desktop ;
- grille des montants stable ;
- état sélectionné perceptible en light et dark ;
- panneau latéral plein écran sur mobile et borné sur desktop.

## Accessibilité

- navigation complète au clavier ;
- focus visible ;
- ordre de tabulation cohérent ;
- `role=status` pour le chargement ;
- `role=alert` pour l’erreur ;
- `role=dialog`, `aria-modal` et `aria-labelledby` pour le panneau ;
- fermeture par Échap et restauration du focus.

## Commandes obligatoires

```bash
cd web
npm test
npm run build

cd ../backend
./mvnw clean verify -B -Dspring.profiles.active=test
```

## Non-régression

- contrat financier STORY-2201 inchangé ;
- aucun endpoint ou DTO backend modifié ;
- téléchargement PDF inchangé ;
- paiement patient toujours bloqué selon le statut et l’état de session ;
- navigation vers le suivi assurance inchangée.