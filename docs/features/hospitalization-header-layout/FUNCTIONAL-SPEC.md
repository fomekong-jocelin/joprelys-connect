# Spécification fonctionnelle — En-tête d’hospitalisation

## Objectif

Rendre l’en-tête du séjour lisible lorsque les actions disponibles et les informations cliniques sont affichées ensemble.

## Règles d’affichage

- Le contexte du séjour reste prioritaire : service, chambre, lit, responsable, date d’admission et motif.
- Les actions sont regroupées dans une zone indépendante et suffisamment espacée.
- Les libellés longs doivent pouvoir revenir à la ligne sans chevaucher une autre information.
- Sur petit écran, le contexte et les actions s’empilent.
- Les actions restent visibles uniquement lorsque l’utilisateur dispose du droit de modification.

## Critères d’acceptation

- Aucun texte ni bouton ne déborde de la carte.
- Les boutons sont séparés par un espacement constant.
- La navigation et les actions existantes conservent leur comportement.
