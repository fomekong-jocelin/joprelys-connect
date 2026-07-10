# Conception technique - Rapprochement de caisse

## Backend

`CashRegisterService` centralise le calcul des soldes et la validation des mouvements. Le contrat de clôture existant est conservé; la valeur `closingBalance` devient explicitement le solde théorique des espèces physiques.

Le service produit un récapitulatif de session contenant les encaissements par moyen de règlement, les sorties espèces et les versements banque. Les contrôles d'accès de consultation de session vérifient le tenant avant de retourner les mouvements.

## Frontend

`BillingCashRegisterComponent` consomme ce récapitulatif. Les calculs locaux sont uniquement une aide d'affichage, la clôture reste décidée par le backend. L'UI affiche les postes de rapprochement et adapte le formulaire pour imposer une référence lors d'un versement banque.

## Sécurité

- Contrôle tenant sur une session demandée par identifiant.
- Validation des entrées serveur; aucun calcul financier n'est confié au client.
- Messages d'erreur génériques côté UI, sans exposition de détails internes.

## Historique

| Date | Changement |
|---|---|
| 2026-07-09 | Création avec la STORY-2110. |
