# BUG-20260710 — Procédure de règlement d'une facture et conflit à l'émission

## Mode

Engineering / correction UX et parcours financier.

## Constat

- L'écran `Facturation & Caisse` affiche les factures dans `Historique des Factures`.
- Le bouton `Régler` ouvre la saisie du montant, du mode de règlement et de la référence.
- L'API de règlement refuse un paiement si aucune session de caisse n'est ouverte (`409 Conflict`).
- L'API de création refuse de recréer une facture pour une visite déjà facturée (`409 Conflict`, message : `Une facture existe déjà pour cette visite.`).
- L'IHM masque le bouton `Régler` lorsque `Invoice.status = PAID`, alors que ce statut représente actuellement le règlement de la part patient et que la part assurance peut rester impayée.
- Le formulaire de création reste actif pour une visite déjà facturée, ce qui pousse l'utilisateur vers le `409`.

## Procédure utilisateur attendue

1. Ouvrir l'onglet `Caisse & Sessions`.
2. Ouvrir une session avec la caisse et le fond initial.
3. Revenir à `Facturation de patients`, rechercher le patient et sélectionner sa visite.
4. Utiliser `Historique des Factures`, puis cliquer sur `Régler` sur la facture existante.
5. Saisir le montant restant, le mode de règlement et la référence si nécessaire, puis valider.
6. Contrôler le reçu et le statut de la créance.

Pour une facture tiers-payant :

1. Régler d'abord la part patient depuis l'historique.
2. Ouvrir `Bordereaux Assurances`.
3. Choisir la convention et la période, puis générer le bordereau.
4. Ouvrir le bordereau en statut `DRAFT`, le marquer comme envoyé.
5. Une fois le virement reçu, ouvrir le bordereau `SENT`, cliquer sur `Enregistrer le Règlement`, saisir la référence bancaire et valider.

## Critères d'acceptation du diagnostic

- [x] Distinguer l'émission d'une facture du règlement d'une facture existante.
- [x] Identifier la précondition de session de caisse ouverte.
- [x] Identifier la cause du `409` observé sur `POST /api/invoices`.
- [x] Documenter le parcours utilisateur et le cas tiers-payant.
- [x] Empêcher l'émission d'une seconde facture pour une visite déjà facturée côté IHM.
- [x] Afficher le règlement patient selon la synthèse de créances et non selon le seul statut technique de facture.
- [x] Clarifier l'accès au règlement assurance via les bordereaux.
- [ ] Vérifier sur une instance locale avec les données de la capture le parcours complet patient puis assurance.

## Impacts et risques

- Aucun changement d'API, de base de données, de sécurité ou de configuration.
- Le bouton `Régler` crée un paiement, un mouvement de caisse et un reçu dans une transaction backend.
- Pour une facture tiers-payant, le règlement patient ne solde pas automatiquement la part assurance ; celle-ci doit être traitée via les bordereaux assurance.

## Tests / vérifications

- Analyse statique du composant Angular de paiement et de `BillingPaymentService` réalisée.
- Analyse statique du flux de session de caisse réalisée.
- [x] `npm run build` : build Angular de production réussi.
- [x] `npm run test -- --watch=false` : 23 fichiers et 111 tests réussis.
- [ ] Test manuel de bout en bout restant à effectuer sur instance locale : facture 100% patient, facture tiers-payant, bordereau puis règlement assurance.

## Impact version / SemVer

PATCH : correction rétrocompatible du parcours utilisateur, sans changement de contrat API ni de modèle de données.

## Reste à faire

- [ ] Afficher le détail du message backend pour les erreurs non prévues.
- [x] La suite de la refonte UX est découpée dans [EPIC-0020](EPIC-0020-billing-cash-workspace-redesign.md), car le correctif ponctuel ne suffisait pas à résoudre la charge cognitive de la page.

## Implémentation réalisée

- La création d'une facture est désactivée et expliquée lorsqu'une facture existe déjà pour la visite sélectionnée.
- L'action `Régler` est pilotée par `InvoiceSettlementSummary.patient`, ce qui permet de régler une part patient même si le statut technique de la facture est `PAID`.
- Les factures avec part assurance restante affichent `Assurance à recouvrer` et un accès direct à l'onglet `Bordereaux Assurances`.
- Le formulaire d'émission indique explicitement qu'il faut utiliser l'historique pour un règlement.
