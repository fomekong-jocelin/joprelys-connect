# Spécification fonctionnelle - Rapprochement de caisse

## Objectif

Permettre au caissier de clôturer une session sur la base des espèces réellement détenues, sans confondre les chèques et les virements avec la monnaie disponible en caisse.

## Règles métier

1. Le fond d'ouverture est toujours un montant en espèces.
2. Seuls les mouvements `IN` payés en `CASH` augmentent le solde espèces.
3. Seuls les mouvements `OUT` payés en `CASH` réduisent le solde espèces.
4. Un `TRANSFER_TO_BANK` représente un dépôt d'espèces et réduit toujours le solde espèces; sa référence de bordereau est obligatoire.
5. Les entrées par chèque et par virement sont suivies dans le récapitulatif mais ne participent pas au montant physique à compter.
6. Un écart entre le montant physique déclaré et le solde espèces théorique requiert une justification avant clôture.

## Parcours

1. Le caissier ouvre une session avec son fond de caisse.
2. Il encaisse les factures et enregistre les sorties ou dépôts banque justifiés.
3. Avant clôture, l'écran présente le détail par moyen de règlement et le solde espèces attendu.
4. Le caissier compte les espèces, saisit le montant constaté et justifie tout écart.

## Hors périmètre

- Décomposition par coupures/pièces.
- Validation électronique DAF des pièces jointes.
- Écritures OHADA et rapprochement bancaire.

## Historique

| Date | Changement |
|---|---|
| 2026-07-09 | Création avec la STORY-2110. |
