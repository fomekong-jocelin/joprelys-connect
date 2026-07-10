# STORY-2201 — Plan de tests du contrat d’état financier

## Backend

### Validation et initialisation

- facture `PENDING` validée : créances patient/assurance créées une seule fois ;
- nouvelle validation refusée ;
- créances créées avec le tenant de la facture ;
- facture assurance 100 % synchronisée vers `PAID` après validation.

### Paiement patient

- paiement partiel : créance patient `PARTIALLY_PAID`, facture `PARTIALLY_PAID` ;
- paiement complet tiers-payant : créance patient `PAID`, facture `PAID`, synthèse `INSURANCE_DUE` ;
- paiement complet sans assurance : facture `SETTLED`, synthèse `SETTLED` ;
- paiement supplémentaire sur `PAID`, `SETTLED`, `CANCELLED`, `PENDING` ou `PROFORMA` refusé ;
- dépassement du solde refusé.

### Assurance

- bordereau exclut les factures `PENDING` ;
- règlement assurance avec patient soldé : facture `SETTLED` ;
- règlement assurance avec patient partiel : facture `PARTIALLY_PAID` ;
- règlement assurance avec patient non payé : facture `VALIDATED` ;
- créance assurance soldée et synthèse cohérente.

### Données historiques

- créance patient manquante reconstruite depuis les paiements existants ;
- création idempotente : aucun doublon de créance ;
- facture déjà `SETTLED` conserve son statut.

### Annulation

- synthèse d’une facture annulée : `CANCELLED` ;
- aucun paiement autorisé.

## Frontend

- type `Invoice.status` accepte `SETTLED` ;
- statut `SETTLED` affiche « Soldée » ;
- bouton d’encaissement masqué pour `SETTLED` ;
- suivi assurance visible uniquement pour `INSURANCE_DUE` ;
- fallback sécurisé pour un statut inconnu.

## Commandes obligatoires

```bash
cd backend
./mvnw clean verify -B -Dspring.profiles.active=test

cd ../web
npm ci
npm test
npm run build
```

## Critères de sortie

- suite backend verte sur H2 et PostgreSQL 16 Testcontainers ;
- tests Angular et build production verts ;
- aucun test désactivé ;
- aucun élargissement RBAC ;
- suivi et changelog mis à jour.
