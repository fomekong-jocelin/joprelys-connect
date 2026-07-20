# Page de connexion premium — Plan de tests

## Tests automatisés Angular

### Rendu initial

- le composant partagé `app-logo` est présent ;
- le mode Personnel est actif par défaut ;
- les boutons de langue affichent 🇫🇷 et 🇬🇧 ;
- le contrôle de thème est présent ;
- le formulaire professionnel reste visible.

### Langues

- cliquer sur le drapeau français appelle `setLang('fr')` ;
- cliquer sur le drapeau britannique appelle `setLang('en')` ;
- l'état actif expose `aria-pressed="true"`.

### Thème

- le thème courant provient de `ThemeService` ;
- cliquer sur le contrôle bascule `light` vers `dark` et inversement ;
- le libellé accessible correspond à l'action proposée.

### Modes d'authentification

- cliquer sur Patient affiche le formulaire patient ;
- revenir sur Personnel réaffiche le formulaire professionnel ;
- le changement de mode réinitialise les erreurs et étapes comme avant.

### Non-régression

- connexion professionnelle puis OTP ;
- redirection vers le `returnUrl` ;
- erreurs d'identifiants ;
- erreurs de livraison OTP ;
- OTP patient ;
- session déjà active et déconnexion.

## Vérifications CI

- `npm run test` ;
- `npm run build` ;
- pipeline backend complet, même si aucun code backend n'est modifié ;
- absence d'Angular Material ;
- aucune URL d'API codée en dur.

## Recette visuelle manuelle

### Viewports

- 320 × 568 ;
- 375 × 812 ;
- 768 × 1024 ;
- 1366 × 768 ;
- 1920 × 1080.

### Variantes

Pour chaque viewport :

- thème clair et sombre ;
- français et anglais ;
- Personnel étape 1 ;
- Personnel OTP ;
- Patient étape 1 ;
- Patient OTP ;
- message d'erreur visible ;
- état de chargement.

## Critères de réussite

- aucun défilement horizontal ;
- aucun bouton ou libellé coupé ;
- formulaire utilisable au clavier ;
- focus visible ;
- contraste lisible ;
- décor non gênant ;
- logo non déformé ;
- aucune carte « Flux clinique synchronisé ».
