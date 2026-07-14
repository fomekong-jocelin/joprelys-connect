# STORY-2402 — Modèle de données

## Extension auth_sessions

Colonnes additives :

- `revoked_by_user_id UUID NULL` : acteur humain quand applicable ;
- `revocation_source VARCHAR(16) NULL` : `SELF`, `ADMIN` ou `SYSTEM`.

Les rotations historiques sont backfillées avec `SYSTEM`. Une session active ne porte ni acteur, ni source, ni motif.

## revoked_access_tokens

Compatibilité bornée des JWT sans `sid` :

- `token_id VARCHAR(64) PRIMARY KEY` ;
- `user_id UUID NULL` ;
- `organization_id UUID NULL` ;
- `revoked_at TIMESTAMP WITH TIME ZONE NOT NULL` ;
- `expires_at TIMESTAMP WITH TIME ZONE NOT NULL` ;
- `reason VARCHAR(32) NOT NULL` ;
- `revoked_by_user_id UUID NULL`.

Le JTI n’est pas un secret d’authentification, mais il reste non exposé. Les lignes sont supprimées après `expires_at`.

## auth_session_audit_events

Journal append-only :

- `id UUID PRIMARY KEY` ;
- `organization_id UUID NULL` ;
- `actor_user_id UUID NULL` ;
- `target_user_id UUID NOT NULL` ;
- `session_id UUID NULL` ;
- `token_family_id UUID NULL` ;
- `event_type VARCHAR(48) NOT NULL` ;
- `reason VARCHAR(64) NULL` ;
- `occurred_at TIMESTAMP WITH TIME ZONE NOT NULL`.

Événements :

- `SESSION_CREATED` ;
- `SESSION_ROTATED` ;
- `SESSION_REVOKED` ;
- `LOGOUT_ALL` ;
- `REFRESH_REPLAY_DETECTED` ;
- `LEGACY_ACCESS_TOKEN_REVOKED`.

## Index

- sessions par utilisateur et état ;
- sessions par famille ;
- JTI par expiration ;
- audit par tenant/date, cible/date et famille/date.

## Contraintes

- migration V67 additive et forward-only ;
- FKs de l’audit sans cascade destructive ;
- aucune donnée de token brut ;
- vocabulaire stocké en `VARCHAR` pour compatibilité H2/PostgreSQL, validé par le domaine Java.