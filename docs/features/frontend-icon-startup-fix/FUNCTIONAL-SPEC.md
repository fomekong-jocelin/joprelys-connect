# Correction du démarrage frontend lié aux icônes

## Problème

Le frontend Angular ne démarre plus à cause d'un problème d'icône. Les utilisateurs et développeurs ne peuvent pas ouvrir l'application web en local tant que la compilation ou le rendu initial échoue.

## Utilisateurs concernés

- Développeurs frontend et full-stack.
- Utilisateurs de recette utilisant le frontend web local ou déployé.

## Objectif

Rétablir le démarrage de l'application web sans changer le parcours utilisateur ni les contrats API.

## Périmètre inclus

- Diagnostic de l'erreur d'icône.
- Correction minimale du composant partagé d'icônes ou des usages invalides.
- Vérification du build Angular.

## Périmètre exclu

- Refonte visuelle.
- Ajout d'une nouvelle bibliothèque d'icônes.
- Changement de design system.
- Changement backend, API ou base de données.

## Critères d'acceptation

- [x] Le frontend compile.
- [x] Les icônes concernées disposent d'un rendu valide via le composant partagé `app-ui-icon`.
- [x] Aucun thème ou branding n'est modifié.
- [x] Aucun contrat API, modèle de données ou comportement métier n'est modifié.

## Cas limites

- Un nom d'icône peut être utilisé par erreur dans un template.
- Un composant peut utiliser un ancien sélecteur d'icône incompatible.
- La correction ne doit pas masquer les futures erreurs de type si un usage invalide est détectable à la compilation.
