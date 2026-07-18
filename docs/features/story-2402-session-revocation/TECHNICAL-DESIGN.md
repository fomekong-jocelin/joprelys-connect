# STORY-2402 — Conception technique

## Architecture

```text
AuthSessionController
  -> ListAuthSessionsUseCase
  -> RevokeAuthSessionUseCase
  -> LogoutCurrentSessionUseCase
  -> LogoutAllSessionsUseCase

JwtAuthenticationFilter
  -> AccessTokenSessionValidator
      -> auth_sessions pour JWT avec sid
      -> revoked_access_tokens pour JWT sans sid

PersistentAuthSessionService
  -> rotation existante
  -> AuthSessionReplayService
  -> AuthSessionAuditPort
```

## Responsabilités

- Controller : validation HTTP, extraction du principal et mapping de réponse.
- Use cases : contrats orientés usage, sans dépendance transport.
- Service de révocation : autorisation métier, tenant, idempotence et transaction.
- Replay service : révocation atomique de famille et audit.
- Validator : décision booléenne d’acceptation d’un access token.
- Repository : requêtes tenantées, verrous et bulk updates paramétrés.
- Audit store : écriture append-only, aucune mise à jour fonctionnelle.

## Autorisation

Le service reçoit un `SessionActor` contenant l’utilisateur, l’établissement, le `sid` courant et les permissions résolues par Spring Security.

- cible = acteur : autorisé ;
- cible différente : `AUTH_SESSION_MANAGE` obligatoire ;
- établissement différent ou absent : réponse identique à une session inconnue.

## Access tokens

### JWT avec sid

Le filtre vérifie :

- session existante ;
- session active à l’instant courant ;
- `user_id` identique au `sub` ;
- `organization_id` identique au claim `org` ;
- compte toujours actif via le RBAC existant.

### JWT sans sid

La compatibilité transitoire s’appuie sur `revoked_access_tokens`. Une entrée existe seulement après logout/révocation explicite et est supprimée après expiration du JWT.

## Rejeu

La rotation conserve un verrou pessimiste. Si la génération verrouillée est déjà `ROTATED`, le service révoque les sessions actives de la famille et écrit l’audit avant de lancer `InvalidAuthSessionException`.

La transaction de refresh utilise `noRollbackFor = InvalidAuthSessionException.class` afin que l’incident de rejeu soit conservé. Les erreurs techniques restent rollbackées.

## Multi-instance

Aucun cache local ne décide de la validité. Toutes les décisions de révocation utilisent PostgreSQL/H2 en test. Un redémarrage ou une autre instance observe immédiatement le même état.

## Conservation

- sessions et audits : conservation configurable déjà bornée par la purge ;
- JTI révoqués : suppression dès que `expires_at` est dépassé ;
- aucun token brut conservé.

## Internationalisation

Le backend retourne des codes stables. Aucun texte visible supplémentaire n’est ajouté au frontend dans #33. Les traductions FR/EN et les états UI appartiennent à #34.

## Purge navigateur

`RefreshTokenCookieManager.clear()` combine l'expiration explicite du cookie HttpOnly avec `Clear-Site-Data: "cache", "cookies", "storage"`. Angular purge en défense en profondeur les deux stockages, les cookies accessibles et les cleanups mémoire (`RbacApiService`, `ActivePatientService`). Une rotation transparente du jeton du même compte retire uniquement l'ancien access token.

## Impact SemVer

MINOR : endpoints additifs, permission additive et migration non destructive V67.
