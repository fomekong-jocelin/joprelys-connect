# Spécification fonctionnelle - Balance Âgée et Relances de Créances (STORY-2114)

## 1. Résumé métier

Le module de recouvrement a pour rôle d'aider le chargé de recouvrement à suivre, trier et relancer de manière efficace les créances impayées de l'établissement, qu'il s'agisse de créances patient (ticket modérateur) ou de créances assurance (Tiers-Payant).

## 2. Objectifs

- Visualiser et trier les créances selon leur ancienneté (concept de **Balance Âgée / Aging Balance**).
- Consigner et consulter l'historique des actions de relance menées sur chaque créance.
- Exclure automatiquement de la liste de travail les créances réglées ou associées à des factures entièrement soldées.

## 3. Règles métier

1. **Calcul des tranches de la balance âgée** :
   L'ancienneté d'une créance est calculée à partir de la date d'émission de la facture :
   - **0 - 30 jours (Sain)** : Créances récentes.
   - **31 - 60 jours (À Relancer)** : Créance en retard modéré. Première relance (généralement téléphonique).
   - **61 - 90 jours (Relance Urgente)** : Retard important. Deuxième relance formelle (courrier, e-mail).
   - **> 90 jours (Contentieux)** : Créance douteuse transférée au service juridique.

2. **Actions de relance autorisées** :
   - Types d'action : `PHONE_CALL` (Appel téléphonique), `EMAIL` (E-mail de relance), `LETTER` (Courrier physique), `VISIT` (Visite sur site).
   - Statuts de relance : `PENDING` (En attente), `PROMISED_PAYMENT` (Promesse de paiement), `DISPUTE` (Litige / Contestation), `UNREACHABLE` (Injoignable).

3. **Visibilité et cycle de vie** :
   - Une créance entièrement réglée (`status = PAID`) ou associée à une facture soldée (`status = SETTLED`) est automatiquement retirée de la balance âgée opérationnelle.
   - L'historique des relances reste archivé et consultable sur la fiche historique de la créance.

## 4. Parcours utilisateur

1. **Consultation de la Balance Âgée** : Le chargé de recouvrement accède à l'onglet "Recouvrement & Créances". Il filtre la liste par tranche d'ancienneté (ex: afficher toutes les créances à relancer entre 31 et 60 jours) ou par type de débiteur (Patient vs Assurance).
2. **Consignation d'une Action** : Il sélectionne une créance, consulte l'historique des relances précédentes, puis clique sur "Relancer".
3. **Saisie du Rapport** : Une boîte de dialogue lui permet de choisir le type de relance (ex: appel téléphonique), le statut obtenu (ex: promesse de paiement pour le 15 du mois) et de rédiger un commentaire.
4. **Mise à jour** : L'action est enregistrée dans l'historique. La date de la dernière relance et le dernier statut sont mis à jour sur la créance dans l'affichage.
