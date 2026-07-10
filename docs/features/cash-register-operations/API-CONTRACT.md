# Contrat API - Rapprochement de caisse

## `GET /api/cash-registers/sessions/active/summary`

Retourne le rapprochement de la session ouverte par l'utilisateur connecté. Accessible aux rôles `ADMIN_CLINIQUE`, `AGENT_ACCUEIL`, `CAISSIER` et `DAF`.

Réponse `200` :

```json
{
  "sessionId": "uuid",
  "openingCash": 10000,
  "cashReceipts": 6000,
  "chequeReceipts": 10000,
  "transferReceipts": 5000,
  "cashExpenses": 1000,
  "bankDeposits": 3000,
  "expectedCash": 12000
}
```

Réponse `404` : aucune session ouverte pour l'utilisateur.

## Règles de validation étendues

`POST /api/cash-registers/movements` refuse désormais :

- un montant nul ou négatif ;
- un type ou un moyen de règlement inconnu ;
- un versement banque dont le moyen n'est pas `CASH` ou sans référence de bordereau.
