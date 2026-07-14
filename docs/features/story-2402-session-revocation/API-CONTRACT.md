# STORY-2402 — Contrat API

Tous les endpoints exigent un access token valide, sauf le refresh déjà public au sens Spring Security.

## GET /api/auth/sessions

Retourne les sessions du compte courant.

```json
[
  {
    "id": "uuid",
    "current": true,
    "clientType": "WEB",
    "deviceLabel": "Chrome / Windows",
    "networkHint": "192.168.1.0/24",
    "createdAt": "2026-07-14T05:00:00Z",
    "lastUsedAt": "2026-07-14T05:20:00Z",
    "expiresAt": "2026-07-14T05:50:00Z",
    "state": "ACTIVE",
    "revocationReason": null
  }
]
```

## GET /api/auth/users/{userId}/sessions

Permission : `AUTH_SESSION_MANAGE`.

Retourne les sessions d’un collaborateur du même établissement. Une cible cross-tenant ou inexistante produit `404 AUTH_SESSION_NOT_FOUND`.

## DELETE /api/auth/sessions/{sessionId}

- session propre : autorisée ;
- autre utilisateur : `AUTH_SESSION_MANAGE` et même établissement ;
- réponse : `204 No Content` ;
- opération répétée : `204 No Content` ;
- cible invisible : `404 AUTH_SESSION_NOT_FOUND`.

## POST /api/auth/logout

Révoque la session courante quand `sid` existe. Pour un JWT historique sans `sid`, persiste le JTI jusqu’à expiration. Efface le cookie de refresh.

Réponse : `204 No Content`.

## POST /api/auth/logout-all

Révoque toutes les sessions actives du compte courant et le JTI courant si le token n’a pas de `sid`. Efface le cookie.

Réponse : `204 No Content`.

## POST /api/auth/refresh

Contrat existant conservé. Nouveau comportement : la réutilisation d’une génération déjà rotatée révoque toute la famille puis retourne :

```json
{
  "type": "about:blank",
  "title": "Invalid authentication session",
  "status": 401,
  "detail": "AUTH_SESSION_INVALID"
}
```

## Sécurité des réponses

Ne jamais retourner : refresh token, hash, JTI, adresse IP complète, acteur interne d’audit ou détails permettant d’identifier une session cross-tenant.