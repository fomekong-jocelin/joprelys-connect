# Conception technique — En-tête d’hospitalisation

## Composant

`HospitalizationStayHeaderComponent` reste un composant de présentation autonome. Il reçoit le séjour, le nom du responsable et l’autorisation d’action via des inputs, puis émet les événements existants via des outputs.

## Mise en page

- La carte `ui-card` reçoit un padding responsive (`p-4`, puis `sm:p-6`) afin que le contenu ne touche jamais sa bordure.
- La zone haute utilise une grille responsive.
- À partir du breakpoint `xl`, la colonne d’information et la colonne d’actions sont séparées.
- La colonne d’actions utilise une grille à deux colonnes : le téléchargement occupe la largeur complète, puis le transfert et la sortie sont répartis sur la seconde ligne.
- Sous `xl`, les zones s’empilent ; sous `sm`, les actions occupent une seule colonne.
- Les libellés des boutons utilisent `whitespace-normal` et `min-w-0` afin de rester contenus.
- Le bouton d’action « Synthèse PDF » utilise une hauteur minimale et un padding alignés sur le composant `ui-button`.
- Le titre du séjour utilise une taille `text-lg` et une hauteur de ligne explicite pour limiter son encombrement.

## Contraintes respectées

- Tailwind CSS v4 et tokens visuels existants.
- Aucun Angular Material.
- Aucun changement API/backend, i18n ou logique métier.
- Rayon et ombre du composant `ui-card` inchangés.
