# Contrat API — Assistant Vocal Clinique & Dictée (MOB-2816)

## Endpoints

### 1. Analyse de dictée clinique (Traitement Backend / Edge)

- **POST** `/api/clinical/assistant/parse-dictation`
- **Headers** : `Authorization: Bearer <token>`, `Content-Type: application/json`
- **Request Body** :
```json
{
  "rawText": "Température 38.5, tension 120/80, pouls 75, SpO2 98%, patient fiévreux avec Céphalées.",
  "language": "fr"
}
```

## 2. Session et analyse utilisées par Flutter

### `GET /api/ai/consultations/{visitId}/session`

- `pendingTranscript` n'est restaurable que lorsque `transcriptStatus` vaut
  `PENDING_REVIEW`.
- Après analyse, `transcriptStatus=ANALYZED`, `pendingTranscript=null` et le champ
  de capture `transcript` n'est plus exposé comme brouillon à reprendre.
- Après effacement, `transcriptStatus=NONE` et aucun transcript n'est restaurable.

### `POST /api/ai/consultations/{visitId}/messages/realtime`

```json
{
  "transcript": "Texte clinique relu",
  "confidence": 1.0,
  "eventId": "mobile-reviewed-..."
}
```

`eventId` reste optionnel pour compatibilité. Lorsqu'il est fourni :

- le même identifiant avec le même payload retourne la première réponse sans
  rappeler le fournisseur IA ;
- le même identifiant avec un autre transcript ou une autre confiance retourne
  `409 AI_REALTIME_EVENT_ID_REUSED`.

### `GET /api/ai/realtime/vitals/{visitId}/intake`

Le endpoint retourne uniquement le working set actif, ordonné par séquence. Les
éléments `CONSUMED` et `DISCARDED` ne sont pas renvoyés lors d'une reprise.

- **Response 200 OK** :
```json
{
  "vitals": {
    "temperature": 38.5,
    "systolic": 120,
    "diastolic": 80,
    "pulse": 75,
    "spo2": 98
  },
  "note": {
    "subjective": "Patient fiévreux avec Céphalées.",
    "objective": "Température 38.5°C, TA 120/80 mmHg, Pouls 75 bpm, SpO2 98%.",
    "assessment": "Syndrome infectieux avec fièvre.",
    "plan": "Paracétamol 1g, surveillance des constantes."
  }
}
```
