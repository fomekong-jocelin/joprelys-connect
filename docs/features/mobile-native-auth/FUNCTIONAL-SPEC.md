# FUNCTIONAL SPEC — MOB-2805 Authentification mobile professionnelle

## Objectif

Permettre à un professionnel Joprelys Connect de s’authentifier dans l’application Flutter, de reprendre une session valide après redémarrage, de la rafraîchir avec le contrat backend existant et de protéger localement l’application par biométrie lorsque cette option est activée.

La biométrie n’authentifie jamais l’utilisateur auprès du serveur. Elle déverrouille uniquement une session professionnelle déjà créée par le backend.

## Parcours de connexion

### Connexion directe

1. le professionnel saisit son adresse e-mail et son mot de passe ;
2. l’application appelle `POST /api/auth/login` ;
3. lorsque `requiresOtp=false`, la réponse complète crée la session professionnelle locale ;
4. l’utilisateur est redirigé vers la surface protégée.

Le mot de passe n’est jamais écrit dans le stockage local ni dans les logs.

### Vérification OTP

Lorsque le backend retourne `requiresOtp=true` :

1. l’application conserve seulement l’adresse e-mail nécessaire au parcours courant ;
2. l’utilisateur saisit le code reçu ;
3. l’application appelle `POST /api/auth/verify-otp` avec `email` et `otpCode` ;
4. seule une réponse d’authentification complète crée une session persistée.

Le code OTP n’est jamais persisté. Annuler le parcours OTP ramène à la connexion.

## Session professionnelle

La session mobile contient uniquement les informations nécessaires au fonctionnement et à l’affichage :

- access token bearer ;
- date d’expiration de l’access token ;
- échéance absolue de session lorsqu’elle est fournie ;
- identifiant de session lorsqu’il est fourni ;
- e-mail, nom et rôle retournés par le backend ;
- préférence locale de verrouillage biométrique.

Ces données sont enregistrées dans le stockage sécurisé natif. Le refresh token reste exclusivement dans le cookie HttpOnly géré par Dio et le CookieJar persistant sécurisé.

## Restauration au démarrage

Au lancement :

1. aucune session stockée : affichage de la connexion ;
2. session encore utilisable : restauration immédiate ;
3. access token expiré mais session absolue encore valide : appel à `POST /api/auth/refresh` ;
4. refus d’authentification : suppression de la session et des cookies ;
5. indisponibilité réseau : conservation de la session locale et affichage d’un état de récupération avec `Réessayer` ou `Supprimer cette session`.

Une panne réseau ne doit pas effacer silencieusement une session potentiellement récupérable.

## Refresh professionnel

MOB-2805 branche le port `ApiSessionAccess` de MOB-2804 sur la session réelle :

- un seul refresh concurrent est exécuté ;
- un token plus récent déjà disponible est réutilisé ;
- la requête rejetée est rejouée au maximum une fois ;
- un `403` ne déclenche ni refresh ni logout ;
- le parcours patient ne peut jamais utiliser ce refresh professionnel.

## Déconnexion

La déconnexion appelle `POST /api/auth/logout` lorsque la session locale existe, puis supprime systématiquement :

- la session chiffrée ;
- les cookies persistés par l’application.

La suppression locale reste obligatoire même si le serveur est temporairement injoignable. L’utilisateur ne doit pas rester connecté localement à cause d’un échec réseau de logout.

## Biométrie

### Activation

L’activation est optionnelle et requiert une vérification biométrique réussie. Le choix est mémorisé dans la session sécurisée.

### Verrouillage

Lorsque l’option est active, l’application se verrouille lorsqu’elle passe en arrière-plan. Le retour dans l’application affiche une page de déverrouillage biométrique.

### Limites

- biométrie uniquement, sans fallback PIN dans ce parcours ;
- aucune génération ou lecture de token par le composant biométrique ;
- en cas d’indisponibilité ou de verrouillage biométrique, l’utilisateur peut se déconnecter ;
- la désactivation est possible depuis la surface authentifiée.

## Navigation protégée

Les guards dirigent chaque état vers une destination unique :

- restauration en cours → `/auth/loading` ;
- non authentifié → `/auth/login` ;
- OTP requis → `/auth/otp` ;
- session biométriquement verrouillée → `/auth/unlock` ;
- restauration réseau impossible → `/auth/recovery` ;
- session authentifiée → `/`.

Aucune page protégée n’est accessible sans session utilisable et déverrouillée.

## Internationalisation et apparence

Tous les libellés sont fournis en français et en anglais via ARB. Les pages utilisent les thèmes light/dark/system et les primitives du design system existant sans modifier `AppTheme` ou `AppDesignTokens`.

### Alignement visuel avec l’application web

Le parcours mobile reprend la hiérarchie de la connexion web sans copier son panneau desktop :

- le thème reste accessible par un contrôle compact à gauche ;
- la langue est sélectionnée par un segment visible `FR | EN` à droite ;
- la marque, le titre et le sous-titre précèdent la carte ;
- la carte est réservée au formulaire ou aux actions du parcours courant ;
- les champs affichent un libellé stable au-dessus de la zone de saisie ;
- les actions principales restent pleine largeur et accessibles au clavier ouvert.

L’accueil professionnel temporaire ne doit afficher aucun identifiant de ticket ou de build. Il présente l’identité de session, le rôle, la protection biométrique et une déconnexion secondaire, tandis que langue et apparence restent dans l’en-tête.

## Sécurité et confidentialité

- aucun mot de passe ou OTP persistant ;
- access token uniquement dans le stockage sécurisé ;
- refresh token uniquement dans le cookie HttpOnly ;
- aucun token, cookie, identifiant personnel ou body métier dans les logs ;
- aucune donnée clinique stockée par ce lot ;
- aucun cache local générique du DPU ;
- aucune authentification patient ajoutée dans ce lot.

## Hors périmètre

- authentification patient complète ;
- création de compte ;
- récupération de mot de passe ;
- dashboard métier final ;
- cache clinique hors ligne ;
- audio natif ;
- pinning TLS ;
- déploiement recette ou production.

## Critères d’acceptation

- [x] login professionnel aligné sur le backend ;
- [x] OTP staff sans persistance du code ;
- [x] session et cookies persistés dans le stockage sécurisé ;
- [x] restauration et refresh réel ;
- [x] panne réseau de restauration non destructive ;
- [x] logout local inconditionnel ;
- [x] biométrie optionnelle limitée au déverrouillage local ;
- [x] guards de navigation ;
- [x] séparation professionnelle/patient ;
- [x] FR/EN et light/dark/system ;
- [x] aucun changement du thème global ;
- [x] composition mobile alignée sur la connexion web et l’accueil professionnel ;
- [x] aucun identifiant technique affiché à l’utilisateur ;
- [ ] gate Flutter complet sur le HEAD final.
