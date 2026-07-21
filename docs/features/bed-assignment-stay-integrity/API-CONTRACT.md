# Contrat API — Intégrité séjour et période

## Endpoints concernés

- `POST /api/hospitalizations`
- `POST /api/spatial/transfers`
- commandes internes de libération lors du statut libre et de la sortie.

## Réponses nominales

Aucun payload n'est modifié.

## Conflit d'affectation

- statut : `409 Conflict` ;
- message : `Une affectation active existe déjà pour ce lit ou cette hospitalisation.` ;
- effet : transaction annulée et rechargement du plan de lits requis.

La réponse ne révèle aucune identité, aucun numéro de séjour et aucun lit concurrent.

## Données invalides internes

Une tentative de clôture antérieure au début est refusée par le domaine puis par la base. Aucun endpoint public ne permet actuellement de fournir directement ces dates.
