# TECHNICAL DESIGN — MOB-2804 Client API Flutter

## 1. Objectif

Créer une fondation réseau transversale sous `mobile/lib/core/network` et la rendre injectable avec Riverpod. Les features conservent leurs DTO, mappers, data sources et repositories ; `core/network` ne connaît aucun modèle patient, clinique ou planning.

## 2. Arborescence

```text
mobile/lib/core/
  config/
    app_config.dart
    app_environment.dart
  network/
    api_client.dart
    api_client_providers.dart
    api_exception.dart
    api_exception_mapper.dart
    api_headers.dart
    api_network_config.dart
    api_recovery_interceptor.dart
    api_request_interceptor.dart
    api_request_policy.dart
    api_session.dart
    session_refresh_coordinator.dart
    trace_id_factory.dart
```

Tests :

```text
mobile/test/
  app_environment_test.dart
  api_exception_mapper_test.dart
  api_client_interceptor_test.dart
  support/queue_http_client_adapter.dart
```

## 3. Configuration runtime

`AppRuntimeConfig` lit :

```text
--dart-define=APP_ENV=dev|recette|prod
--dart-define=API_BASE_URL=https://...
```

Règles :

- URL absolue HTTP(S) ;
- aucun user info, query ou fragment ;
- suppression du slash terminal ;
- HTTPS obligatoire en recette/prod ;
- défaut développement Android Emulator : `http://10.0.2.2:8080`.

`--dart-define` ne contient aucun secret.

## 4. Composition Dio

`apiDioProvider` construit une instance avec :

1. `BaseOptions` et timeouts explicites ;
2. `CookieManager` + `CookieJar` en mémoire ;
3. `ApiRequestInterceptor` ;
4. `ApiRecoveryInterceptor`.

`apiClientProvider` expose ensuite `ApiClient`. Un écran ne doit jamais observer `apiDioProvider`.

## 5. Politique de requête

Chaque appel `ApiClient` reçoit un `ApiRequestPolicy` stocké dans `RequestOptions.extra`.

### Authentification

- `none` : aucun bearer ;
- `optional` : bearer si session disponible ;
- `required` : bearer si disponible, puis réponse backend explicite sinon.

### Retry

- `never` ;
- `safeMethod` pour GET/HEAD/OPTIONS ;
- `idempotencyKey` pour une écriture explicitement rejouable.

Une requête ne peut être rejouée qu’une fois. `Stream` et `FormData` sont exclus car leur corps n’est pas nécessairement réutilisable.

## 6. Enrichissement des requêtes

`ApiRequestInterceptor` ajoute :

- format JSON ;
- code langue `fr` ou `en` ;
- trace ID créé par `SecureTraceIdFactory` ;
- bearer depuis `ApiSessionAccess` ;
- clé d’idempotence issue de la policy.

La factory de trace utilise `Random.secure()` et produit un identifiant technique sans PII.

## 7. Session et refresh

`ApiSessionAccess` est un port :

```dart
Future<ApiSessionSnapshot?> current();
Future<ApiSessionSnapshot?> refresh({String? rejectedAccessToken});
Future<void> expire();
```

MOB-2804 fournit `AnonymousApiSessionAccess`. MOB-2805 remplacera ce provider par l’implémentation authentifiée.

`SessionRefreshCoordinator` conserve une seule `Future` de refresh en vol. Toutes les réponses `401` professionnelles concurrentes attendent cette même future.

Ordre de récupération :

```text
401
 ↓
session actuelle ?
 ↓
token déjà renouvelé par une autre requête ? → replay unique
 ↓
refresh professionnel coordonné
 ↓
nouveau token valide ? → replay unique
 ↓
sinon expire session
```

Les routes `/api/auth/*` ne déclenchent pas cette boucle afin d’éviter un refresh récursif.

Une session patient (`ApiSessionKind.patient`) ne possède pas la capacité de refresh professionnel.

## 8. Erreurs

`ApiExceptionMapper` accepte :

### Enveloppe Joprelys

```json
{
  "error": {
    "code": "ACCESS_DENIED",
    "message": "...",
    "trace_id": "trc_...",
    "action": "...",
    "required_scope": "..."
  },
  "detail": "..."
}
```

### RFC 7807

```json
{
  "title": "Invalid authentication session",
  "detail": "AUTH_SESSION_INVALID",
  "status": 401
}
```

### Transport

- `connectionTimeout`, `sendTimeout`, `receiveTimeout`, `transformTimeout` ;
- `connectionError` ;
- `badCertificate` ;
- `cancel`.

Le modèle produit `ApiFailureKind`, `code`, `message`, `statusCode`, `traceId`, métadonnées d’autorisation et indicateur `retryable`.

## 9. Résilience

`ApiRecoveryInterceptor` effectue :

- un refresh/replay d’authentification au maximum ;
- un retry transitoire au maximum ;
- retry uniquement pour 408/502/503/504 ou timeout/connexion ;
- aucune relance automatique sur 403 ;
- aucune relance automatique sur 429 dans ce ticket, même si l’erreur est marquée rejouable pour décision de la couche supérieure.

## 10. Cookies

Le backend professionnel actuel place le refresh token dans un cookie HttpOnly. `dio_cookie_manager` respecte ce contrat. Le `CookieJar` de MOB-2804 est volatile : fermeture de l’application = disparition du cookie.

La persistance éventuelle sera implémentée en MOB-2805 avec stockage protégé, effacement à la déconnexion et règles de durée conformes au backend. Aucun refresh token n’est transformé en valeur Dart lisible par les features.

## 11. Sécurité et observabilité

Interdits :

- log des headers Authorization/Cookie ;
- log des bodies patient/clinique ;
- clé fournisseur IA dans l’application ;
- URL de production non HTTPS ;
- retry aveugle d’une écriture ;
- dépendance de présentation vers Dio.

Les logs futurs pourront contenir : méthode, route normalisée, statut, durée, environnement et trace ID, jamais le body ni les secrets.

## 12. Frontière thème

Aucun fichier de `core/theme` n’est modifié. La couche réseau ne définit aucun token, couleur, widget ni règle d’ergonomie.

## 13. Rollback

Le rollback retire les fichiers `core/network`, les providers et les dépendances Dio/cookies. Aucune migration backend ou base de données n’est associée.
