# BUG-20260721-LANGUAGE-FLAG-SCOPE

- GitHub : #94
- Type : régression UI
- Priorité : P1
- Statut : IN REVIEW
- Périmètre : Angular / CSS global / i18n

## Constat

La feuille globale `web/src/styles/language-flags.css` appliquait les pseudo-éléments de drapeaux à tout bouton enfant direct d'un conteneur `.select-none` dans `app-shell`.

La liste des suggestions d'examens biologiques utilise également `.select-none`. Le premier examen recevait donc le drapeau français et le dernier le drapeau britannique.

## Cause

Sélecteur trop générique :

```css
app-shell .select-none > button
```

Une classe utilitaire de sélection de texte ne constitue pas un contrat sémantique de sélecteur de langue.

## Correction

Les drapeaux du shell sont désormais limités aux deux emplacements réellement dédiés à la langue :

- barre supérieure desktop ;
- panneau de navigation mobile.

Les pages publiques, juridiques, mot de passe oublié et préférences de confidentialité conservent leurs règles spécifiques.

## Non-régression

Un test de structure CSS vérifie :

- l'absence du sélecteur global historique ;
- la présence des sélecteurs desktop et mobile ciblés.

## Critères d'acceptation

- [x] aucun drapeau dans les suggestions d'examens ;
- [x] drapeaux conservés dans le sélecteur desktop ;
- [x] drapeaux conservés dans le sélecteur mobile ;
- [x] pages publiques et juridiques inchangées ;
- [x] test de non-régression ajouté ;
- [ ] tests Angular verts ;
- [ ] build Angular production vert ;
- [ ] Maven strict vert.
