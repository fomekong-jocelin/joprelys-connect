# Contrat API — Notes de Consultation Clinique (MOB-2814)

## Endpoints

### 1. Récupérer la note de consultation

- **GET** `/api/visits/{id}/consultation-notes`
- **Headers** : `Authorization: Bearer <token>`, `Accept-Language: fr|en`
- **Response 200 OK** :
```json
{
  "subjective": "Douleurs abdominales intenses depuis 2h.",
  "objective": "Abdomen souple mais douloureux en fosse iliaque droite.",
  "assessment": "Suspicion d'appendicite aiguë.",
  "plan": "Demande d'échographie abdominale + bilan sanguin.",
  "updatedAt": "2026-07-30T22:30:00Z"
}
```

### 2. Enregistrer la note de consultation

- **POST** `/api/visits/{id}/consultation-notes`
- **Headers** : `Authorization: Bearer <token>`, `Content-Type: application/json`
- **Request Body** :
```json
{
  "subjective": "Douleurs abdominales intenses depuis 2h.",
  "objective": "Abdomen souple mais douloureux en fosse iliaque droite.",
  "assessment": "Suspicion d'appendicite aiguë.",
  "plan": "Demande d'échographie abdominale + bilan sanguin."
}
```
- **Response 200 OK** :
```json
{
  "subjective": "Douleurs abdominales intenses depuis 2h.",
  "objective": "Abdomen souple mais douloureux en fosse iliaque droite.",
  "assessment": "Suspicion d'appendicite aiguë.",
  "plan": "Demande d'échographie abdominale + bilan sanguin.",
  "updatedAt": "2026-07-30T22:30:00Z"
}
```
