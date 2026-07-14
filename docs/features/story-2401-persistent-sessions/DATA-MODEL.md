# STORY-2401 — Modèle de données

## Table `auth_sessions`

| Colonne | Type | Null | Rôle |
|---|---|---:|---|
| `id` | UUID | non | identifiant de la génération de session |
| `user_id` | UUID | non | compte du personnel |
| `organization_id` | UUID | oui | établissement au moment de l’émission ; nul pour certains comptes plateforme |
| `token_family_id` | UUID | non | identifiant stable de la session logique |
| `refresh_token_hash` | VARCHAR(64) | non | SHA-256 hex du token opaque |
| `client_type` | VARCHAR(32) | non | `WEB` pour cette story |
| `user_agent` | VARCHAR(160) | oui | valeur normalisée et tronquée |
| `ip_prefix` | VARCHAR(64) | oui | IP masquée, jamais adresse complète persistée |
| `created_at` | TIMESTAMP TZ | non | émission de cette génération |
| `last_used_at` | TIMESTAMP TZ | non | dernier usage de cette génération |
| `absolute_expires_at` | TIMESTAMP TZ | non | limite non prolongeable de la famille |
| `idle_expires_at` | TIMESTAMP TZ | non | limite d’inactivité de cette génération |
| `revoked_at` | TIMESTAMP TZ | oui | fin d’usage de cette génération |
| `revocation_reason` | VARCHAR(32) | oui | `ROTATED`, `EXPIRED`, `LOGOUT`, etc. |
| `replaced_by_session_id` | UUID | oui | génération créée par la rotation |
| `version` | BIGINT | non | verrou optimiste |

## Contraintes

- PK sur `id` ;
- FK `user_id -> users(id)` ;
- FK `organization_id -> organizations(id)` lorsque non nul ;
- FK différée logique `replaced_by_session_id -> auth_sessions(id)` ;
- unique sur `refresh_token_hash` ;
- `absolute_expires_at > created_at` ;
- `idle_expires_at > created_at` ;
- `idle_expires_at <= absolute_expires_at` ;
- cohérence révocation : raison et date renseignées ensemble ;
- une ligne ne peut pas se remplacer elle-même.

## Index

- unique `refresh_token_hash` pour la rotation ;
- `(user_id, revoked_at, absolute_expires_at)` pour le futur catalogue #33 ;
- `(token_family_id, created_at)` pour l’historique et la détection de rejeu ;
- `(absolute_expires_at, idle_expires_at)` pour la purge ;
- `organization_id` pour les opérations administratives futures.

## Cycle de vie

### Émission

```text
id=S1
family=F1
revoked_at=NULL
replaced_by=NULL
```

### Rotation

Ancienne ligne :

```text
S1.revoked_at=now
S1.revocation_reason=ROTATED
S1.replaced_by_session_id=S2
```

Nouvelle ligne :

```text
id=S2
family=F1
absolute_expires_at=S1.absolute_expires_at
idle_expires_at=min(now + inactivityTTL, absolute_expires_at)
```

## Secret

Le token brut existe uniquement :

- dans la mémoire du générateur ;
- dans le résultat applicatif transitoire ;
- dans le cookie HTTP.

Il n’existe jamais dans :

- une colonne ;
- une entité sérialisée ;
- un audit ;
- un message d’erreur ;
- un log.

## Migration

V66 est additive et ne reprend aucune donnée historique : les access tokens JWT existants restent valides jusqu’à leur expiration, mais aucune session persistante n’est inventée pour eux.