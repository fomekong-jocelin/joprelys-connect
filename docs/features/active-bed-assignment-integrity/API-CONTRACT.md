# Contrat API — Intégrité des affectations actives de lit

## Endpoints concernés

- `POST /api/hospitalizations`
- `POST /api/spatial/hospitalizations/{hospitalizationId}/transfer/{newBedId}`

## Compatibilité

Aucun payload de requête ou réponse nominale n'est modifié.

## Conflit

Lorsqu'une affectation active existe déjà pour le lit au moment du flush :

- statut : `409 Conflict` ;
- message métier : `Une affectation active existe déjà pour ce lit ou cette hospitalisation.` ;
- la transaction complète est annulée.

Le client doit recharger le plan de lits avant de proposer une nouvelle sélection. Aucun retry automatique sur le même lit n'est recommandé.

## Sécurité

Les permissions existantes ne changent pas. La réponse ne révèle ni l'identité ni le séjour de l'occupant concurrent.
