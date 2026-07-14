# STORY-2401 — Contrat API

## POST `/api/auth/login`

Le corps reste inchangé.

### Réponse authentifiée `200`

```json
{
  "accessToken": "eyJ...",
  "tokenType": "Bearer",
  "expiresAt": "2026-07-14T08:15:00Z",
  "sessionExpiresAt": "2026-07-21T07:45:00Z",
  "sessionId": "0a51a846-5ac5-4f82-98fe-d3e31217ea32",
  "email": "agent@example.com",
  "name": "Agent Accueil",
  "role": "AGENT_ACCUEIL",
  "requiresOtp": false,
  "otpCode": null
}
```

En-tête :

```http
Set-Cookie: joprelys_refresh=<opaque>; Path=/api/auth; HttpOnly; Secure; SameSite=Lax; Max-Age=<seconds>
```

### Réponse nécessitant OTP `200`

Aucun cookie de refresh n’est créé avant la validation OTP.

## POST `/api/auth/verify-otp`

Le corps reste inchangé. Après succès, la réponse et le cookie sont identiques au login authentifié.

## POST `/api/auth/refresh`

Endpoint public au sens Spring Security, mais protégé par la possession du cookie opaque.

### Requête

- aucun corps ;
- cookie de refresh obligatoire ;
- `User-Agent` et adresse réseau utilisés uniquement pour des métadonnées normalisées.

### Réponse `200`

```json
{
  "accessToken": "eyJ...",
  "tokenType": "Bearer",
  "expiresAt": "2026-07-14T08:45:00Z",
  "sessionExpiresAt": "2026-07-21T07:45:00Z",
  "sessionId": "e6d242f7-5411-41fd-8b81-2da17e4ec90d",
  "email": "agent@example.com",
  "name": "Agent Accueil",
  "role": "AGENT_ACCUEIL",
  "requiresOtp": false,
  "otpCode": null
}
```

Un nouveau cookie remplace le précédent.

### Erreur `401`

Toutes les erreurs de refresh utilisent un détail générique et suppriment le cookie :

```json
{
  "type": "about:blank",
  "title": "Unauthorized",
  "status": 401,
  "detail": "AUTH_SESSION_INVALID"
}
```

Le serveur ne distingue pas publiquement token absent, expiré, consommé ou utilisateur désactivé.

## Compatibilité

- les champs historiques ne sont ni renommés ni supprimés ;
- `sessionExpiresAt` et `sessionId` sont additifs ;
- le refresh token n’est jamais un champ JSON ;
- les clients qui ignorent le cookie conservent le comportement historique jusqu’à expiration de l’access token.

## Sécurité

- aucune valeur de cookie dans les logs ;
- aucune organisation fournie par le client ;
- le tenant est vérifié depuis la session et le compte ;
- le cookie est host-only et limité à `/api/auth` ;
- CORS doit rester explicite lorsque credentials sont autorisés.