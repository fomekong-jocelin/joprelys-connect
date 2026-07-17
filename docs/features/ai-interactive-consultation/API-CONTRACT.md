# Consultation IA interactive — Contrat API

Base : `/api/ai/consultations/{visitId}`

Tous les endpoints exigent un utilisateur authentifié avec rôle `MEDECIN` ou `ADMIN_CLINIQUE` et autorité `CLINICAL_WRITE`.

## SessionView enrichie

```json
{
  "sessionId": "uuid",
  "visitId": "uuid",
  "status": "ACTIVE",
  "revision": 4,
  "expiresAt": "2026-07-17T22:00:00Z",
  "acceptedDraft": {},
  "messages": [],
  "pendingTranscription": null,
  "clarifications": [],
  "proposals": []
}
```

## Créer une transcription

`POST /api/ai/consultations/{visitId}/transcriptions`

Content-Type autorisé : `audio/webm`, `audio/mp4`, `audio/mpeg`, `audio/wav`.

Réponse `201` :

```json
{
  "id": "uuid",
  "originalText": "Le patient...",
  "editedText": "Le patient...",
  "mimeType": "audio/webm",
  "status": "PENDING_REVIEW",
  "createdAt": "..."
}
```

Aucune analyse clinique n’est déclenchée à cette étape.

## Modifier la transcription

`PATCH /api/ai/consultations/{visitId}/transcriptions/{transcriptionId}`

```json
{
  "text": "texte corrigé",
  "expectedRevision": 4
}
```

Réponse `200` : transcription mise à jour + nouvelle révision de session.

## Confirmer la transcription

`POST /api/ai/consultations/{visitId}/transcriptions/{transcriptionId}/confirm`

```json
{
  "expectedRevision": 5
}
```

Réponse `200` : `SessionView` complète après analyse.

## Abandonner la transcription

`POST /api/ai/consultations/{visitId}/transcriptions/{transcriptionId}/discard`

Réponse `200` : `SessionView` avec transcription `DISCARDED` ou retirée selon politique finale.

## Envoyer un message texte

L’endpoint existant reste :

`POST /api/ai/consultations/{visitId}/messages/text`

```json
{
  "text": "Corrige : la douleur est à droite et non à gauche",
  "expectedRevision": 5,
  "clarificationId": null
}
```

Réponse `200` : `SessionView` complète avec nouveau message et propositions en attente.

## Répondre à une clarification

`POST /api/ai/consultations/{visitId}/clarifications/{clarificationId}/responses`

```json
{
  "answer": "50 mg deux fois par jour",
  "expectedRevision": 6
}
```

Réponse `200` : session avec clarification résolue et nouvelles propositions éventuelles.

## Décider une proposition

### Accepter

`POST /api/ai/consultations/{visitId}/proposals/{proposalId}/accept`

```json
{
  "expectedRevision": 7
}
```

### Rejeter

`POST /api/ai/consultations/{visitId}/proposals/{proposalId}/reject`

Même payload.

Réponse `200` : session mise à jour.

## Décision globale

- `POST /api/ai/consultations/{visitId}/proposals/accept-all`
- `POST /api/ai/consultations/{visitId}/proposals/reject-all`

Payload : `expectedRevision`.

## Erreurs

| Statut | Code | Cas |
|---:|---|---|
| 400 | `AI_MESSAGE_INVALID` | texte vide ou invalide |
| 401 | `AUTH_REQUIRED` | authentification absente |
| 403 | standard sécurité | permission insuffisante |
| 409 | `AI_SESSION_EXPIRED` | session absente/expirée |
| 409 | `AI_SESSION_CONFLICT` | révision obsolète |
| 409 | `VISIT_NOT_ACTIVE` | visite non active |
| 409 | `AI_TRANSCRIPTION_STATE_INVALID` | confirmation/édition impossible |
| 404 | `AI_TRANSCRIPTION_NOT_FOUND` | transcription inconnue |
| 404 | `AI_CLARIFICATION_NOT_FOUND` | clarification inconnue |
| 404 | `AI_PROPOSAL_NOT_FOUND` | proposition inconnue |
| 413 | `AI_AUDIO_TOO_LARGE` | audio > 10 MiB |
| 415 | `AI_AUDIO_TYPE_UNSUPPORTED` | type non autorisé |
| 422 | `AI_OUTPUT_INVALID` | sortie fournisseur non exploitable |
| 503 | `AI_UNAVAILABLE` | fournisseur indisponible |

Les erreurs utilisent le format Problem Detail déjà présent dans le projet.

## Compatibilité

- Aucun endpoint existant n’est supprimé dans cette feature.
- Les nouveaux champs des réponses sont additifs.
- L’ancien endpoint audio reste compatible pendant la transition ; le nouveau frontend utilise le parcours transcription séparée.
- Toute dépréciation future sera annoncée dans le changelog et la documentation API.
