# API Contract — Constantes Vitales (Visites)

## Endpoints

### 1. Enregistrer les constantes
`POST /api/visits/{id}/vitals`
- Autorisation : `VISIT_VITALS_WRITE`

**Body (JSON)**:
```json
{
  "temperature": 37.8,
  "weight": 70.5,
  "height": 175,
  "pulse": 78,
  "systolic": 120,
  "diastolic": 80,
  "spo2": 98,
  "glycemia": 1.10,
  "respiratoryRate": 16,
  "painScale": 2
}
```

### 2. Récupérer les constantes
`GET /api/visits/{id}/vitals`
- Autorisation : `VISIT_READ`

**Response (JSON)**:
```json
{
  "temperature": 37.8,
  "weight": 70.5,
  "height": 175,
  "pulse": 78,
  "systolic": 120,
  "diastolic": 80,
  "spo2": 98,
  "glycemia": 1.10,
  "respiratoryRate": 16,
  "painScale": 2,
  "bmi": 23.02
}
```
