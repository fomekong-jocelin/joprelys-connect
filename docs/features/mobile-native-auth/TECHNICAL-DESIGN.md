# TECHNICAL DESIGN — MOB-2805 Authentification mobile

## Vue d’ensemble

MOB-2805 consomme les primitives réseau de MOB-2804 et ajoute une feature `auth` isolée :

```text
UI auth / router
        |
        v
AuthController (Riverpod AsyncNotifier)
        |
        v
AuthSessionManager implements ApiSessionAccess
        |                         |
        v                         v
AuthGateway / AuthApi       AuthSessionStore
        |                         |
        v                         v
Dio auth + CookieManager    FlutterSecureStorage
        |
        v
Backend /api/auth/* + cookie HttpOnly
```

Le client API métier principal reçoit `AuthSessionManager` via l’override de `apiSessionAccessProvider`. Le Dio dédié à l’authentification ne contient volontairement pas `ApiRecoveryInterceptor`, afin qu’un appel `/api/auth/refresh` ne puisse jamais déclencher récursivement son propre refresh.

## Organisation des fichiers

```text
lib/
  core/
    security/secure_storage.dart
    network/secure_cookie_storage.dart
  features/auth/
    domain/professional_session.dart
    data/
      auth_api.dart
      auth_session_store.dart
      secure_auth_session_store.dart
    application/
      auth_controller.dart
      auth_lifecycle_lock.dart
      auth_providers.dart
      auth_session_manager.dart
      biometric_authenticator.dart
    presentation/
      pages/
      widgets/
```

Les DTO et comportements d’authentification restent dans la feature. `core` ne reçoit que les adaptateurs transversaux de stockage sécurisé et de cookie.

## Contrat backend

### Login

```http
POST /api/auth/login
Content-Type: application/json

{
  "email": "...",
  "password": "..."
}
```

### OTP

```http
POST /api/auth/verify-otp
Content-Type: application/json

{
  "email": "...",
  "otpCode": "..."
}
```

### Refresh

```http
POST /api/auth/refresh
Cookie: refresh_token=<HttpOnly>
```

### Logout

```http
POST /api/auth/logout
Authorization: Bearer <access-token>
Cookie: refresh_token=<HttpOnly>
```

La réponse d’authentification attendue contient :

- `accessToken` ;
- `tokenType=Bearer` ;
- `expiresAt` ;
- `sessionExpiresAt` optionnel ;
- `sessionId` optionnel ;
- `email` ;
- `name` ;
- `role` ;
- `requiresOtp`.

Le parseur échoue explicitement si une session complète est attendue mais qu’un champ obligatoire est absent ou invalide.

## Stockage sécurisé

### Session

`SecureAuthSessionStore` sérialise `ProfessionalSession` en JSON dans `FlutterSecureStorage`. Une valeur illisible ou invalide est supprimée plutôt que partiellement acceptée.

Le modèle `toString()` n’expose jamais l’access token.

### Cookies

`SecureCookieStorage` implémente le contrat `cookie_jar.Storage` :

- clés de cookie transformées en clés namespacées base64url ;
- valeurs stockées par `FlutterSecureStorage` ;
- index chiffré des clés écrites ;
- écritures et suppressions sérialisées ;
- `deleteAll` supprime toutes les clés suivies et l’index.

`PersistCookieJar` conserve les cookies de session nécessaires au redémarrage. Les features ne lisent jamais le refresh token.

## Clients Dio

### Dio principal

Intercepteurs :

1. `CookieManager` ;
2. `ApiRequestInterceptor` ;
3. `ApiRecoveryInterceptor`.

Le gestionnaire de session réel est injecté dans `apiSessionAccessProvider` au bootstrap.

### Dio auth

Intercepteurs :

1. `CookieManager` ;
2. `ApiRequestInterceptor` avec un accès lecture seule au stockage de session.

Aucun recovery interceptor n’est ajouté. Le login, l’OTP et le refresh utilisent une politique publique ; le logout exige le bearer mais interdit retry et refresh automatique.

## Cycle de session

### Login

- purge de toute session/cookie obsolète ;
- appel backend ;
- challenge OTP : aucune session écrite ;
- succès complet : écriture atomique de la session.

### Restore

- absence : `null` ;
- échéance absolue dépassée : purge ;
- access token valide : retour immédiat ;
- access token expiré : refresh ;
- 401/403 au refresh : purge ;
- erreur réseau/transport : `SessionRecoveryUnavailable`, sans purge.

### Refresh depuis MOB-2804

`AuthSessionManager.refresh(rejectedAccessToken)` :

- retourne un token stocké plus récent lorsqu’il est utilisable ;
- sinon appelle le refresh backend ;
- conserve la préférence biométrique ;
- réécrit la session ;
- laisse `SessionRefreshCoordinator` garantir un seul appel concurrent.

### Logout

L’échec distant est absorbé volontairement. Le `finally` supprime toujours session et cookies locaux. Ce comportement privilégie la sécurité locale de l’appareil ; la session serveur expirera selon sa propre politique lorsque le backend est indisponible.

## États Riverpod

`AuthController` expose :

- `unauthenticated` ;
- `otpRequired` ;
- `authenticated` ;
- `locked` ;
- `recoveryError`.

L’`AsyncValue` encadre le chargement initial et les opérations en cours. Les erreurs attendues sont converties en codes stables traduits par la présentation.

## Composition UI alignée web/mobile

La correction visuelle reste strictement dans la présentation :

- `AuthPreferencesBar` expose le thème à gauche et un segment FR/EN à droite ;
- `AppBrandLockup` centralise le lockup logo + `Connect` pour les pages publiques et protégées ;
- `AuthShell` place marque, titre et sous-titre hors de la surface actionnable ;
- `AppTextField` fournit un mode de libellé externe réutilisable, sans logique d’authentification ;
- `AppButton` fournit une variante destructive secondaire pour les sorties de session ;
- `FoundationPage` compose des surfaces identité et sécurité à partir de la session déjà validée.

Les widgets ne calculent ni rôle, ni permission, ni validité de session. Ils rendent l’état produit par `AuthController` et émettent uniquement les intentions existantes.

## Navigation

`GoRouter` écoute `authControllerProvider` via un `ChangeNotifier` dédié. Le redirect calcule une destination canonique par état et évite les boucles en retournant `null` lorsque la route courante est déjà correcte.

La route `/` est protégée. Les routes d’authentification sont :

- `/auth/loading` ;
- `/auth/login` ;
- `/auth/otp` ;
- `/auth/unlock` ;
- `/auth/recovery`.

## Biométrie

`LocalBiometricAuthenticator` encapsule `local_auth` :

- vérification du support et des biométries enrôlées ;
- `biometricOnly=true` ;
- `persistAcrossBackgrounding=true` ;
- mapping des exceptions en résultat métier ;
- aucun accès au stockage ou au réseau.

Android utilise `FlutterFragmentActivity`, la permission `USE_BIOMETRIC` et une API minimale 24. iOS conserve une cible 13.0 et déclare `NSFaceIDUsageDescription`.

## Verrouillage de cycle de vie

`AuthLifecycleLock` observe le cycle Flutter. Sur `paused` ou `detached`, une session authentifiée avec biométrie active passe à `locked`. Le retour passe par la route de déverrouillage.

## Frontières de sécurité

- le mot de passe et l’OTP restent uniquement en mémoire pendant la saisie ;
- aucun logging applicatif de requête auth ;
- aucun fallback biométrique délivrant un token ;
- aucun partage de session avec le futur parcours patient ;
- `403` reste un refus d’autorisation ;
- aucune donnée clinique dans la feature auth ;
- aucune modification de `AppTheme`, `AppDesignTokens` ou du contrat backend.

## Décisions différées

- authentification patient ;
- récupération/changement de mot de passe ;
- révocation distante multi-appareils ;
- pinning TLS ;
- politique de ré-authentification forte pour actions sensibles ;
- instrumentation de sécurité centralisée sans données personnelles.
