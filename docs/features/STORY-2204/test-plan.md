# STORY-2204 — Plan de tests

## Backend
- génération tenantée du bordereau ;
- transitions nominales `DRAFT → SENT → RECEIVED → ACCEPTED → PARTIALLY_PAID → SETTLED` ;
- rejet depuis `RECEIVED` ou `ACCEPTED` avec motif obligatoire ;
- refus des transitions invalides ;
- refus d'un montant accepté supérieur au montant réclamé ;
- refus d'un paiement nul, négatif ou supérieur au reste ;
- cumul de plusieurs paiements ;
- allocation sans dépassement sur les créances assurance ;
- refus inter-tenant ;
- refus des rôles non financiers ;
- migration H2 et PostgreSQL 16.

## Frontend
- chargement, erreur/retry et état vide ;
- filtres statut, convention, période et recherche ;
- synthèse des montants ;
- actions disponibles selon le statut et le rôle ;
- formulaires réception, acceptation, rejet et paiement ;
- paiement partiel puis solde ;
- affichage light/dark et responsive ;
- navigation clavier et libellés accessibles.

## Validation globale
- `./mvnw clean verify -B -Dspring.profiles.active=test` ;
- `npm test` ;
- `npm run build`.
