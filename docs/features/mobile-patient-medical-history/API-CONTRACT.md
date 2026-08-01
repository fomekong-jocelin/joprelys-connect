# Contrat API — Historique Médical & Chronologie (MOB-2810)

## Endpoints

### 1. Obtention du dossier historique d'un patient
- **Méthode** : `GET`
- **Path** : `/api/patients/{patientId}/medical-history`
- **Réponse Succès (200 OK)** :
```json
{
  "patientId": "pat-102",
  "patientName": "MOMO Allons",
  "patientDpu": "DPU-JOP-20260725-000001",
  "antecedents": [
    { "type": "MEDICAL", "description": "Hypertension Artérielle essentielle", "diagnosedYear": 2021 },
    { "type": "SURGICAL", "description": "Appendicectomie", "diagnosedYear": 2018 }
  ],
  "allergies": [
    { "allergen": "Pénicilline", "severity": "SEVERE", "reaction": "Choc anaphylactique / Éruption" }
  ],
  "pastVisits": [
    {
      "id": "vis-20260720-001",
      "visitNumber": "VIS-20260720-000001",
      "date": "2026-07-20T10:30:00Z",
      "practitionerName": "Dr. Jean DUPONT",
      "chiefComplaint": "Syndrome grippal et céphalées",
      "temperature": 38.5,
      "systolic": 120,
      "diastolic": 80,
      "pulse": 78
    }
  ]
}
```
