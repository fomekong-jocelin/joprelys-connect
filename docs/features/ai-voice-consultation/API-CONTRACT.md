# API-CONTRACT — Assistant vocal IA de consultation

## 1. Vue d’ensemble

Contrat cible v1. Les endpoints IA manipulent exclusivement un brouillon
éphémère. La persistance finale reste `POST /api/visits/{visitId}/consultation`.

## 2. Authentification commune

- JWT requis.
- Rôles : `MEDECIN`, `ADMIN_CLINIQUE`.
- Permission : `CLINICAL_WRITE`.
- Visite du tenant courant et statut `EN_COURS`.
- Feature flag IA actif pour la clinique.

## 3. Endpoints

### `GET /api/visits/{visitId}/qrcode`

Retourne un PNG contenant l’URL web de consultation.

| Réponse | Signification |
|---:|---|
| 200 `image/png` | QR généré |
| 401 | Non authentifié |
| 403 | Rôle interdit |
| 404 | Visite absente ou autre tenant |
| 409 | Visite non active |

### `POST /api/ai/consultations/{visitId}/sessions`

```json
{
  "sessionId": "uuid",
  "visitId": "uuid",
  "status": "ACTIVE",
  "expiresAt": "2026-07-17T15:30:00Z",
  "draft": {},
  "assistantMessage": "Décrivez les symptômes et l’examen clinique."
}
```

### `GET /api/ai/consultations/{visitId}/session`

Retourne la session active du médecin courant. Ne retourne jamais d’audio.

### `POST /api/ai/consultations/{visitId}/messages/text`

```json
{
  "text": "Le patient présente des céphalées depuis 48 heures."
}
```

### `POST /api/ai/consultations/{visitId}/messages/audio`

- Corps binaire ; aucun nom de fichier utilisateur.
- MIME : `audio/webm`, `audio/mp4`, `audio/mpeg`, `audio/wav`.
- Limite applicative : 10 MiB ; durée UX : 120 secondes maximum.

### Réponse commune

```json
{
  "sessionId": "uuid",
  "transcript": "Texte relu par le médecin, absent si message texte",
  "draft": {
    "symptoms": "Céphalées évoluant depuis 48 heures"
  },
  "changedFields": ["symptoms"],
  "assistantMessage": "Quelle est l’intensité de la douleur sur 10 ?",
  "needsClarification": true,
  "expiresAt": "2026-07-17T15:30:00Z"
}
```

### `DELETE /api/ai/consultations/{visitId}/session`

Supprime le brouillon et l’historique éphémère. Réponse `204`.

## 4. Erreurs

| HTTP | Code fonctionnel | Cause |
|---:|---|---|
| 400 | `AI_MESSAGE_INVALID` | texte vide/payload invalide |
| 401 | `AUTH_REQUIRED` | JWT absent/invalide |
| 403 | `AI_ACCESS_DENIED` | rôle/permission/tenant |
| 404 | `VISIT_NOT_FOUND` | visite absente ou masquée |
| 409 | `VISIT_NOT_ACTIVE` | statut différent de `EN_COURS` |
| 409 | `AI_SESSION_EXPIRED` | session expirée |
| 413 | `AI_AUDIO_TOO_LARGE` | audio > limite |
| 415 | `AI_AUDIO_TYPE_UNSUPPORTED` | MIME hors allowlist |
| 422 | `AI_OUTPUT_INVALID` | sortie provider non conforme |
| 429 | `AI_RATE_LIMITED` | quota interne/provider |
| 503 | `AI_UNAVAILABLE` | feature/provider indisponible |

Les erreurs suivent Problem Detail et n’exposent ni corps provider, ni prompt,
ni contenu clinique.

## 5. Compatibilité

- Nouveaux endpoints : changement rétrocompatible MINOR.
- Aucun champ du contrat de consultation existant n’est modifié.
- Aucun client ne dépend des messages textuels d’erreur.

## 6. Historique

| Date | Auteur | Changement |
|---|---|---|
| 2026-07-17 | Codex | Contrat cible initial |
