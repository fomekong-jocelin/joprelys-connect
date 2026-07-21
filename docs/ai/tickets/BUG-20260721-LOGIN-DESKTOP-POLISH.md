# BUG-20260721-LOGIN-DESKTOP-POLISH

- GitHub : #84
- Type : bug UI/UX
- Priorité : P1
- Statut : IN REVIEW
- Stack : Angular / CSS / responsive / accessibilité

## Constat

Sur Chrome Windows desktop, les emoji de drapeaux peuvent être rendus sous forme de codes régionaux. Le sélecteur de langue affiche alors visuellement `FR FR` et `GB EN` au lieu de véritables drapeaux.

Le panneau institutionnel gauche de la page de connexion est également trop large et trop chargé : titre de connexion répété, grands cercles, bouclier incliné et espace visuel mal équilibré.

## Décision

- conserver le socle mobile-first et toute la logique d’authentification existante ;
- remplacer le rendu visuel des emoji par des drapeaux SVG embarqués dans le CSS afin d’éviter toute dépendance à la police emoji du système ;
- réduire le panneau gauche à environ 35–37 % de la largeur desktop ;
- supprimer visuellement la copie redondante du titre de connexion dans ce panneau ;
- adopter un fond institutionnel bleu nuit avec une composition beaucoup plus sobre ;
- conserver le logo officiel, le bouclier de sécurité et les trois repères de marque ;
- ne pas ajouter de bibliothèque ou de dépendance externe.

## Fichiers

- `web/src/app/auth/login.desktop-polish.css`
- `web/angular.json`

## Critères d’acceptation

- [x] drapeau France stable sans dépendance emoji ;
- [x] drapeau Royaume-Uni stable sans dépendance emoji ;
- [x] contrôle FR/EN compact ;
- [x] panneau gauche limité à environ 35–37 % sur écran large ;
- [x] titre de connexion non répété dans le panneau gauche ;
- [x] aucun changement des parcours Personnel/Patient ;
- [x] aucun changement de l’authentification, de l’OTP ou des sessions ;
- [ ] tests Angular verts ;
- [ ] build Angular production vert ;
- [ ] build Maven strict vert ;
- [ ] recette visuelle Chrome Windows à 1024, 1366, 1440 et 1920 px.

## Recette visuelle

1. Ouvrir la page de connexion sur Chrome Windows.
2. Vérifier que les contrôles affichent un drapeau français avec `FR` et un drapeau britannique avec `EN`.
3. Vérifier l’absence de rendu `FR FR` ou `GB EN`.
4. Contrôler l’équilibre du panneau gauche et du formulaire aux largeurs cibles.
5. Vérifier light/dark, FR/EN, Personnel/Patient.
