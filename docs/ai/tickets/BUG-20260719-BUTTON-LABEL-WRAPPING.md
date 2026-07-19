# BUG-20260719-BUTTON-LABEL-WRAPPING

## Référence

- GitHub : #72
- Type : bug UI transversal
- Priorité : haute
- Périmètre : Angular Web / design system
- Impact SemVer : PATCH

## Problème

Les libellés de certains boutons sont rendus sur deux lignes lorsque l'espace disponible diminue. Le bouton change alors de hauteur, désaligne les groupes d'actions et détériore la lecture des parcours métier.

Le défaut appartient au design system : il ne doit pas être corrigé écran par écran.

## Décision

Le libellé interne d'un bouton standard reste sur une seule ligne. Lorsque plusieurs actions ne tiennent plus dans leur conteneur, le conteneur fait revenir les boutons entiers à la ligne ou les empile sur mobile ; le texte d'un bouton n'est jamais coupé arbitrairement.

La règle est appliquée dans une feuille globale dédiée du design system, chargée après `styles.css` par Angular.

## Actions

- [x] Documenter le comportement attendu.
- [ ] Ajouter la règle globale `white-space: nowrap`.
- [ ] Empêcher le rétrécissement interne des boutons avec `flex-shrink: 0`.
- [ ] Stabiliser la hauteur typographique avec une ligne explicite.
- [ ] Enregistrer la feuille globale dans `angular.json`.
- [ ] Mettre à jour `DESIGN.md`.
- [ ] Vérifier les thèmes light/dark et les largeurs 320, 375, 768 et 1440 px.
- [ ] Exécuter les tests Angular et le build de production.

## Critères d'acceptation

- [ ] Aucun bouton `.ui-button` ne coupe son libellé sur deux lignes.
- [ ] Les boutons avec icône et texte restent verticalement alignés.
- [ ] Les groupes d'actions peuvent revenir à la ligne bouton par bouton.
- [ ] Aucun défilement horizontal global n'est introduit.
- [ ] Les variantes primaire, secondaire, danger et disabled restent inchangées visuellement.
- [ ] Le comportement est identique en français et en anglais.
- [ ] Le comportement est identique en light et dark.

## Risques

- Un conteneur d'actions configuré sans retour à la ligne peut révéler un débordement préexistant. Ce conteneur doit alors être corrigé ; la règle de non-coupure du libellé ne sera pas annulée pour masquer le défaut de layout.

## Reste à faire

- Validation CI.
- Recette visuelle responsive sur les écrans prioritaires.
