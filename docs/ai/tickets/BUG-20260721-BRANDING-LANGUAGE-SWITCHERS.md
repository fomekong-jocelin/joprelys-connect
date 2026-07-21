# BUG-20260721-BRANDING-LANGUAGE-SWITCHERS

- GitHub : #86
- Type : bug UI/UX et cohérence de marque
- Priorité : P1
- Statut : IN REVIEW
- Stack : Angular / CSS / responsive / branding / i18n

## Constat

La recette de la page de connexion après la PR #85 montre encore quatre défauts :

- le logo couleur est posé dans une carte blanche sur le panneau bleu ;
- le panneau de gauche n’explique pas la proposition de valeur Joprelys ;
- des traits décoratifs sans fonction restent visibles en bas du panneau et sous le formulaire ;
- les pages juridiques et certains autres écrans utilisent encore des emoji de drapeaux, non fiables sur Chrome Windows.

## Décision

1. Utiliser la ressource officielle `logo_white_blue_bg.png` sur le panneau institutionnel bleu.
2. Supprimer la carte, la bordure et l’ombre entourant le logo.
3. Présenter la marque avec le slogan officiel « Parce qu’elle est précieuse, nous innovons pour la protéger. » et sa déclinaison anglaise, ainsi qu’un court texte institutionnel FR/EN.
4. Simplifier l’illustration sécurité en supprimant cartes, orbites, repères et traits inutiles.
5. Centraliser les drapeaux France/Royaume-Uni sous forme de SVG CSS.
6. Appliquer ces drapeaux aux sélecteurs de langue de :
   - la connexion ;
   - toutes les pages juridiques ;
   - la page de préférences de confidentialité ;
   - la récupération de mot de passe ;
   - le shell applicatif desktop et mobile.

## Fichiers

- `web/src/app/auth/login.branding-refinement.css`
- `web/src/styles/language-flags.css`
- `web/angular.json`

## Critères d’acceptation

- [x] logo blanc officiel directement sur le fond bleu ;
- [x] aucun encadrement du logo ;
- [x] slogan officiel affiché en français et décliné en anglais ;
- [x] proposition de valeur Joprelys affichée en français et en anglais ;
- [x] suppression visuelle des traits et repères décoratifs signalés ;
- [x] drapeaux vectoriels indépendants de la police système ;
- [x] drapeaux appliqués aux sélecteurs publics et authentifiés ;
- [x] aucun changement des parcours Personnel/Patient, OTP ou session ;
- [ ] tests Angular verts sur le dernier commit ;
- [ ] build Angular production vert sur le dernier commit ;
- [ ] Maven strict vert sur le dernier commit ;
- [ ] recette visuelle light/dark, FR/EN, mobile et desktop.

## Recette manuelle

1. Ouvrir `/` en 1366, 1440 et 1920 px.
2. Vérifier le logo blanc sans carte sur le panneau bleu.
3. Vérifier le slogan officiel FR puis sa version EN.
4. Vérifier l’absence des traits sous le formulaire et en bas du panneau gauche.
5. Ouvrir `/legal/terms` et `/legal/privacy-preferences`.
6. Vérifier les drapeaux réels France/Royaume-Uni.
7. Se connecter et vérifier le sélecteur du shell sur desktop et mobile.
8. Vérifier le sélecteur de `/forgot-password`.
