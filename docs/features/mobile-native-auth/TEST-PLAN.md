# TEST PLAN — MOB-2805 Authentification mobile

## Objectif

Vérifier que l’authentification professionnelle Flutter respecte le contrat backend, protège correctement les secrets, restaure la session sans comportement destructif et interdit l’accès aux routes protégées lorsque la session n’est pas utilisable ou déverrouillée.

## Niveaux de test

### Tests unitaires API

Fichier : `mobile/test/auth_api_test.dart`

Scénarios :

- login envoyé à `/api/auth/login` avec `email` normalisé et `password` inchangé ;
- politique publique sans bearer obligatoire, refresh ou retry automatique ;
- réponse `requiresOtp=true` transformée en challenge OTP sans session ;
- OTP envoyé à `/api/auth/verify-otp` avec `email` et `otpCode` ;
- réponse complète transformée en `ProfessionalSession` ;
- dates UTC, identifiant de session et rôle conservés ;
- réponse de refresh incomplète rejetée comme réponse malformée ;
- logout configuré sans retry ni récupération automatique.

### Tests unitaires gestionnaire de session

Fichier : `mobile/test/auth_session_manager_test.dart`

Scénarios :

- purge de l’ancienne session et des cookies avant un nouveau login ;
- écriture de la session après authentification complète ;
- absence d’écriture lors d’un challenge OTP ;
- restauration directe d’un access token encore valide ;
- refresh d’un access token expiré ;
- conservation de la préférence biométrique après refresh ;
- suppression session/cookies après refus d’authentification ;
- conservation de la session locale lors d’une indisponibilité réseau ;
- logout local réussi même si l’appel distant échoue ;
- réutilisation d’un token plus récent lors de requêtes concurrentes.

### Analyse statique

- aucun import inutilisé ;
- aucun accès nullable non contrôlé ;
- aucune règle de lint désactivée ;
- API Riverpod, GoRouter, Dio, `flutter_secure_storage` et `local_auth` compatibles avec les versions verrouillées.

### Build Android

- résolution des plugins natifs ;
- `minSdk >= 24` ;
- `MainActivity` basée sur `FlutterFragmentActivity` ;
- permission `USE_BIOMETRIC` présente ;
- génération d’un APK debug.

### Préparation iOS

Contrôles statiques dans ce lot :

- cible iOS 13.0 conservée ;
- `NSFaceIDUsageDescription` présente ;
- aucun code Android importé dans les couches Dart métier.

Le build iOS signé n’est pas exécuté par la CI Linux.

## Scénarios fonctionnels manuels

### AUTH-01 — Connexion sans OTP

1. démarrer sans session ;
2. vérifier la redirection vers `/auth/login` ;
3. saisir des identifiants professionnels valides ;
4. vérifier l’accès à `/` ;
5. vérifier nom, e-mail et rôle ;
6. redémarrer l’application ;
7. vérifier la restauration de session.

Résultat attendu : aucune nouvelle saisie si la session est encore valide.

### AUTH-02 — Connexion avec OTP

1. utiliser un compte avec OTP actif ;
2. vérifier la redirection vers `/auth/otp` ;
3. saisir un code incorrect ;
4. vérifier le maintien sur la page avec message localisé ;
5. saisir un code valide ;
6. vérifier la création de session et l’accès protégé.

Résultat attendu : aucune session persistée avant le succès OTP.

### AUTH-03 — Access token expiré

1. disposer d’une session avec cookie de refresh valide et access token expiré ;
2. relancer l’application ;
3. vérifier l’appel unique de refresh ;
4. vérifier la restauration de la surface protégée.

Résultat attendu : nouveau bearer persisté, préférence biométrique conservée.

### AUTH-04 — Refresh refusé

1. invalider le cookie serveur ;
2. relancer l’application ou provoquer un `401` ;
3. vérifier la suppression locale de la session et des cookies ;
4. vérifier la redirection login.

### AUTH-05 — Panne réseau au démarrage

1. conserver une session expirée mais potentiellement rafraîchissable ;
2. couper le réseau ;
3. relancer ;
4. vérifier l’écran de récupération ;
5. rétablir le réseau et cliquer `Réessayer`.

Résultat attendu : la session n’est pas supprimée pendant la panne et peut être récupérée ensuite.

### AUTH-06 — Logout hors ligne

1. être authentifié ;
2. couper le réseau ;
3. cliquer `Se déconnecter` ;
4. vérifier la redirection login ;
5. redémarrer hors ligne.

Résultat attendu : aucune restauration locale malgré l’échec distant du logout.

### AUTH-07 — Activation biométrique

1. être authentifié sur un appareil configuré ;
2. activer le verrouillage biométrique ;
3. réussir le contrôle système ;
4. passer l’application en arrière-plan ;
5. revenir ;
6. vérifier la page verrouillée puis le déverrouillage.

### AUTH-08 — Biométrie indisponible

1. utiliser un appareil sans biométrie enrôlée ou temporairement verrouillée ;
2. tenter l’activation ou le déverrouillage ;
3. vérifier le message adapté ;
4. vérifier que le logout reste accessible.

### AUTH-09 — Refus 403

1. appeler une ressource protégée sans scope suffisant ;
2. recevoir `403` ;
3. vérifier l’absence de refresh ;
4. vérifier que la session reste authentifiée.

### AUTH-10 — Séparation patient/professionnel

1. injecter une session `ApiSessionKind.patient` dans un test d’intercepteur ;
2. provoquer `401` ;
3. vérifier qu’aucun refresh professionnel n’est appelé.

Ce scénario reste couvert par la fondation MOB-2804 et ne doit pas régresser après le branchement MOB-2805.

## Matrice UI

Tester les pages login, OTP, unlock et recovery :

- français et anglais ;
- thème clair et sombre ;
- Android 360 px, 390 px et tablette ;
- clavier ouvert sur e-mail, mot de passe et OTP ;
- taille de police système augmentée ;
- lecteur d’écran sur boutons, champs et erreurs.

Vérifications visuelles supplémentaires :

- thème compact à gauche et segment FR/EN à droite ;
- marque lisible sans écrasement de l’asset officiel ;
- titre et sous-titre hors de la carte ;
- libellés de champs stables et focus visible ;
- absence de l’identifiant `MOB-2805` dans l’accueil ;
- identité, rôle, biométrie et déconnexion lisibles sans bouton destructif dominant ;
- absence de débordement à 360 px et avec une taille de texte augmentée.

## Contrôles de confidentialité

- rechercher toute occurrence de valeurs réelles de mot de passe, OTP, JWT ou cookie dans le dépôt ;
- vérifier que les fixtures utilisent uniquement des valeurs fictives ;
- vérifier que `ProfessionalSession.toString()` n’expose pas l’access token ;
- vérifier l’absence d’intercepteur de log des bodies auth ;
- vérifier que les erreurs UI affichent des messages localisés, pas la réponse brute serveur.

## Commandes CI

```bash
cd mobile
flutter pub get
dart format --output=none --set-exit-if-changed lib test
flutter analyze
flutter test
flutter build apk --debug
```

## Definition of Done

- [x] tests API ajoutés ;
- [x] tests de session ajoutés ;
- [x] configuration Android biométrique ajoutée ;
- [x] préparation iOS ajoutée ;
- [x] scénarios manuels documentés ;
- [x] contrôles de confidentialité documentés ;
- [x] format, analyse et 53 tests Flutter verts sur le HEAD local ;
- [x] build web Flutter vert ;
- [x] goldens login/accueil sombre à `393 × 852` validées dans `design-qa.md` ;
- [ ] APK debug vert sur le HEAD final ; exécution locale bloquée par l’accès
  sandbox au cache Gradle 8.14 et par l’absence de réseau ;
- [ ] recette réelle Android avec backend recette après autorisation explicite de déploiement/configuration.
