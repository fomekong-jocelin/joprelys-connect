# Contrat API — Triage ABCDE

## Ajouter une réévaluation

`POST /api/emergencies/{emergencyId}/triage-assessments`

Permission : `EMERGENCY_WRITE` ou rôle clinique historique autorisé.

### Requête

```json
{
  "triageLevel": "ORANGE",
  "hemodynamicStatus": "UNSTABLE",
  "bpSystolic": 92,
  "bpDiastolic": 58,
  "heartRate": 124,
  "temperature": 37.8,
  "abcdeAssessment": {
    "airwayStatus": "PATENT",
    "breathingStatus": "DISTRESS",
    "circulationStatus": "COMPROMISED",
    "disabilityStatus": "RESPONDS_TO_VOICE",
    "exposureStatus": "TRAUMA",
    "respiratoryRate": 30,
    "oxygenSaturation": 89,
    "gcsScore": 12,
    "painScore": 7,
    "recommendedOrientation": "RESUSCITATION",
    "clinicalNotes": "Aggravation respiratoire, oxygène démarré.",
    "assessedAt": "2026-07-13T15:10:00Z"
  }
}
```

### Réponse `201 Created`

```json
{
  "id": "uuid",
  "emergencyId": "uuid",
  "assessmentType": "REASSESSMENT",
  "sequenceNumber": 2,
  "triageLevel": "ORANGE",
  "hemodynamicStatus": "UNSTABLE",
  "airwayStatus": "PATENT",
  "breathingStatus": "DISTRESS",
  "circulationStatus": "COMPROMISED",
  "disabilityStatus": "RESPONDS_TO_VOICE",
  "exposureStatus": "TRAUMA",
  "bpSystolic": 92,
  "bpDiastolic": 58,
  "heartRate": 124,
  "respiratoryRate": 30,
  "oxygenSaturation": 89,
  "temperature": 37.8,
  "gcsScore": 12,
  "painScore": 7,
  "recommendedOrientation": "RESUSCITATION",
  "clinicalNotes": "Aggravation respiratoire, oxygène démarré.",
  "assessedAt": "2026-07-13T15:10:00Z",
  "assessedByUserId": "uuid",
  "createdAt": "2026-07-13T15:10:02Z"
}
```

L’en-tête `Location` pointe vers `/api/emergencies/{emergencyId}/triage-assessments/{assessmentId}`.

## Lire l’historique

`GET /api/emergencies/{emergencyId}/triage-assessments`

Permission : `EMERGENCY_READ` ou rôle clinique historique autorisé.

Réponse : tableau ordonné par `sequenceNumber` croissant.

## Extension compatible de création d’urgence

Les payloads existants de :

- `POST /api/emergencies` ;
- `POST /api/emergencies/provisional`

acceptent désormais un champ optionnel dans l’objet de triage :

```json
{
  "abcdeAssessment": {
    "airwayStatus": "PATENT",
    "breathingStatus": "ADEQUATE",
    "circulationStatus": "STABLE",
    "disabilityStatus": "ALERT",
    "exposureStatus": "NO_CRITICAL_FINDING"
  }
}
```

Si le champ est absent, l’évaluation initiale est tout de même créée avec les cinq axes à `NOT_ASSESSED`.

## Valeurs autorisées

### Airway

`NOT_ASSESSED`, `PATENT`, `AT_RISK`, `OBSTRUCTED`

### Breathing

`NOT_ASSESSED`, `ADEQUATE`, `DISTRESS`, `FAILURE`

### Circulation

`NOT_ASSESSED`, `STABLE`, `COMPROMISED`, `SHOCK`

### Disability

`NOT_ASSESSED`, `ALERT`, `RESPONDS_TO_VOICE`, `RESPONDS_TO_PAIN`, `UNRESPONSIVE`

### Exposure

`NOT_ASSESSED`, `NO_CRITICAL_FINDING`, `TRAUMA`, `HYPOTHERMIA`, `HYPERTHERMIA`, `OTHER`

### Orientation recommandée

`RESUSCITATION`, `OPERATING_ROOM`, `HOSPITALIZATION`, `CONSULTATION`, `TRANSFER`, `DISCHARGE`, `DEATH`

## Erreurs

- `400` : validation des valeurs ou heure future ;
- `401` : authentification absente ;
- `403` : permission insuffisante ;
- `404 EMERGENCY_NOT_FOUND` : urgence inexistante ou autre tenant ;
- `409 EMERGENCY_ALREADY_STABILIZED` : réévaluation interdite après stabilisation.