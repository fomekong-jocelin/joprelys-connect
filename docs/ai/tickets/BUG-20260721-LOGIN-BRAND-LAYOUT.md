# BUG-20260721-LOGIN-BRAND-LAYOUT

- GitHub : #90
- Type : bug UI/UX et régression CSS
- Priorité : P1
- Statut : IN REVIEW
- Stack : Angular / CSS / responsive / branding

## Constat

Après la fusion de la PR #89, les bénéfices du panneau produit sont inclinés et se chevauchent. Une ancienne ellipse décorative reste visible et le slogan officiel recouvre partiellement la liste. Le logo Joprelys Connect est par ailleurs trop petit pour jouer son rôle de premier repère de marque.

## Cause

`login.desktop-polish.css` définissait historiquement `.login-brand-visual::before` comme une ellipse absolue avec bordure et rotation. La feuille de raffinement chargée ensuite a remplacé le contenu du pseudo-élément sans réinitialiser toutes les propriétés héritées.

## Correction

1. Réinitialiser explicitement `position`, `inset`, dimensions, bordure, rayon, fond, ombre et `transform` du pseudo-élément.
2. Présenter les trois bénéfices dans un panneau vertical sobre et lisible.
3. Séparer le slogan avec une bordure supérieure et un espacement stable.
4. Porter le logo blanc officiel à une hauteur desktop de 68 à 84 px.
5. Renforcer le nom Connect et l’espacement du bloc de marque.
6. Conserver la composition `100dvh`, avec scroll de sécurité sous 760 px.

## Fichiers

- `web/src/app/auth/login.branding-refinement.css`
- `DESIGN.md`

## Critères d’acceptation

- [x] propriétés d’ellipse et rotation entièrement neutralisées ;
- [x] bénéfices verticaux, alignés et lisibles ;
- [x] slogan isolé sous les bénéfices ;
- [x] logo blanc rendu prioritaire et immédiatement identifiable ;
- [x] règles de prévention ajoutées à `DESIGN.md` ;
- [x] aucune modification des parcours Personnel/Patient, OTP ou session ;
- [ ] tests Angular verts ;
- [ ] build Angular production vert ;
- [ ] Maven strict vert ;
- [ ] recette visuelle à 1366×768, 1440×900 et 1920×1080.

## Recette manuelle

1. Déployer la branche sur la recette.
2. Ouvrir la connexion à 1366×768.
3. Vérifier que le logo est le premier élément identifié sur le panneau bleu.
4. Vérifier que les trois bénéfices sont horizontaux, non inclinés et non superposés.
5. Vérifier que le slogan reste sous la liste sans chevauchement.
6. Répéter en anglais et en thème sombre.
7. Vérifier les modes Personnel et Patient.
8. Confirmer l’absence de scroll à partir de 760 px de hauteur et son retour sous ce seuil.
