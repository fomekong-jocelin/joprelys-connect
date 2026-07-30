# TEST PLAN — MOB-2804 Client API Flutter

## Objectif

Vérifier que le client mobile applique les contrats réseau, d’authentification et de résilience sans fuite de secret, sans retry dangereux et sans dépendance de présentation vers Dio.

## Tests de configuration

### CFG-01 — Développement Android

- `APP_ENV=dev` ;
- URL `http://10.0.2.2:8080/` ;
- attendu : URL normalisée sans slash terminal.

### CFG-02 — Recette / production

- URL HTTP ;
- attendu : configuration refusée.

### CFG-03 — URL non sûre

- user info, query, fragment ou schéma non HTTP(S) ;
- attendu : configuration refusée.

## Tests d’enrichissement

### HDR-01 — Requête authentifiée

- session professionnelle valide ;
- attendu : `Accept`, `Accept-Language`, `X-Trace-Id`, bearer.

### HDR-02 — Requête publique

- policy `none` ;
- attendu : aucun header Authorization même si une session existe.

### HDR-03 — Idempotence

- écriture avec clé ;
- attendu : `Idempotency-Key` présent.

## Tests de session

### AUTH-01 — Refresh professionnel

- première réponse 401 ;
- refresh retourne un nouveau token ;
- attendu : une seule relance avec nouveau bearer.

### AUTH-02 — 401 concurrents

- deux requêtes échouent simultanément ;
- attendu : un seul appel `refresh()` et deux replays.

### AUTH-03 — Patient

- session patient + 401 ;
- attendu : aucune tentative de refresh professionnel, session expirée.

### AUTH-04 — Refus 403

- attendu : aucun refresh et aucun logout automatique.

### AUTH-05 — Boucle interdite

- 401 après replay ou erreur `/api/auth/*` ;
- attendu : aucune seconde récupération.

## Tests de retry

### RETRY-01 — GET transitoire

- première réponse 503, seconde 200 ;
- attendu : une relance maximum, même trace ID.

### RETRY-02 — POST non idempotent

- réponse 503 ;
- attendu : aucune relance.

### RETRY-03 — Écriture idempotente

- clé fournie et première réponse 503 ;
- attendu : une relance et même clé.

### RETRY-04 — Body non rejouable

- `Stream` ou `FormData` ;
- attendu : aucune relance automatique.

## Tests d’erreurs

### ERR-01 — Enveloppe canonique

- vérifier code, message, trace ID, action, scope et kind.

### ERR-02 — ProblemDetail

- `detail=AUTH_SESSION_INVALID` ;
- attendu : code stable et type unauthenticated.

### ERR-03 — Timeouts Dio

- connexion, envoi, réception et transformation ;
- attendu : `REQUEST_TIMEOUT`, retryable.

### ERR-04 — Connexion absente

- attendu : `NETWORK_UNAVAILABLE`, retryable.

### ERR-05 — Annulation / TLS

- attendu : catégories dédiées, aucun retry TLS automatique.

## Contrôles architecture et sécurité

- aucun import Dio dans `features/*/presentation` ;
- aucun changement dans `AppTheme` ou `AppDesignTokens` ;
- aucun token ou cookie dans les messages d’exception ;
- CookieJar volatile uniquement dans MOB-2804 ;
- aucune donnée patient dans les fixtures ;
- aucun cache local DPU.

## Gate CI

Depuis `mobile/` :

```bash
flutter pub get
dart format --output=none --set-exit-if-changed .
flutter analyze
flutter test
flutter build apk --debug
```

## Preuve runtime intermédiaire

Run GitHub Actions #2046 / ID `30522369209` sur `0c2670a3545bedc60d2f241e9452bc7a6a594de8` :

- résolution des dépendances : verte ;
- format : vert ;
- analyse : verte ;
- tests Flutter : verts ;
- APK debug : vert ;
- backend/frontend non concernés et correctement ignorés.

## Validation appareil ultérieure

Avant le premier écran connecté :

- dev Android Emulator vers `10.0.2.2` ;
- recette HTTPS sur appareil physique ;
- perte/reprise Wi-Fi et données mobiles ;
- session professionnelle expirée ;
- session patient expirée ;
- changement FR/EN et contrôle `Accept-Language` ;
- absence de secrets dans Logcat.

## Critère de sortie

La fusion exige un gate complet vert sur le HEAD final, documentation comprise. La recette réseau physique devient obligatoire avec MOB-2805 et le premier écran authentifié.
