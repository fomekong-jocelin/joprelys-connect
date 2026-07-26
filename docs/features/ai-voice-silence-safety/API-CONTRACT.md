# Sécurité silence et repli vocal IA — Contrat API

## Dépôt d'une transcription Realtime à relire

```http
POST /api/ai/consultations/{visitId}/transcriptions/realtime
Content-Type: application/json
```

```json
{
  "transcript": "Texte transcrit par le canal Realtime"
}
```

Réponse `200 OK` :

```json
{
  "sessionId": "uuid",
  "transcript": "Texte transcrit par le canal Realtime",
  "status": "PENDING_REVIEW",
  "expiresAt": "2026-07-26T10:00:00Z"
}
```

Ce endpoint ne lance jamais le modèle de structuration et ne crée aucune révision.
Le texte doit ensuite être confirmé avec le contrat existant :

```http
POST /api/ai/consultations/{visitId}/transcriptions/analyze
```

## Erreurs

| Statut | Code | Signification |
|---:|---|---|
| 400 | `AI_MESSAGE_INVALID` | transcription vide |
| 409 | `AI_TRANSCRIPT_REVIEW_REQUIRED` | une transcription attend déjà une décision |
| 409 | code clarification/révision existant | une validation clinique bloque un nouveau tour |
| 413 | `AI_TRANSCRIPT_TOO_LARGE` | texte supérieur à la limite |
| 422 | `AI_TRANSCRIPTION_LOW_CONFIDENCE` | confiance fournisseur connue sous le seuil |

## Compatibilité

- les endpoints existants ne sont pas supprimés ;
- `POST /messages/audio` reste disponible pour les clients historiques ;
- le client Angular courant n'utilise plus l'analyse audio immédiate ;
- aucune modification d'autorisation : `CLINICAL_WRITE` et session active restent requis.
