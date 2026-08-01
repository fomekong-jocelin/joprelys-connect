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
