# Contrat API — Cohérence établissement des affectations de lit

## Contrats inchangés

Aucun endpoint, payload ou champ de réponse n'est ajouté ou retiré.

## Nouveau refus

Les commandes d'admission ou de transfert qui tentent d'utiliser un lit d'un autre établissement répondent :

- HTTP : `409 Conflict` ;
- message : `Le lit sélectionné n'appartient pas à l'établissement du séjour.`

Le message ne contient ni identifiant, ni nom de patient, ni information sur l'établissement concurrent.

Les violations tardives de contrainte continuent d'être traduites en 409 générique par le service d'affectation.
