# STORY-1104 — Import FHIR DiagnosticReport : Contrat API

## Endpoint

```
POST /api/public/lab-integration/fhir/diagnostic-report
```

## Sécurité

| Mécanisme     | Détail                                               |
|---------------|------------------------------------------------------|
| Type          | API Key statique                                     |
| En-tête       | `X-API-KEY: <valeur>`                                |
| Configuration | `joprelys.lab-integration.api-key` (application.yml) |
| Défaut dev    | `lab-partner-secret-token`                           |

## Requête

### En-têtes

| En-tête        | Obligatoire | Description               |
|----------------|-------------|---------------------------|
| `X-API-KEY`    | Oui         | Clé d'API du laboratoire  |
| `Content-Type` | Oui         | `application/json`        |

### Corps

```json
{
  "fhirJson": "<DiagnosticReport FHIR R4 JSON en tant que chaîne>",
  "validatorName": "Dr. Sophie Martin"
}
```

| Champ           | Type   | Obligatoire | Description                                          |
|-----------------|--------|-------------|------------------------------------------------------|
| `fhirJson`      | string | Oui         | Document FHIR R4 DiagnosticReport sérialisé en JSON  |
| `validatorName` | string | Non         | Nom du validateur ; défaut : `"Système FHIR"`        |

### Exemple de `fhirJson` (contenu du champ)

```json
{
  "resourceType": "DiagnosticReport",
  "subject": { "reference": "ServiceRequest/REQ-2024-001" },
  "effectiveDateTime": "2024-06-15T08:00:00Z",
  "issued": "2024-06-15T10:30:00Z",
  "result": [
    {
      "code": { "text": "Hémoglobine" },
      "valueQuantity": { "value": 14.2, "unit": "g/dL" },
      "referenceRange": [ { "text": "12.0 - 16.0 g/dL" } ],
      "interpretation": [ { "text": "NORMAL" } ],
      "note": [ { "text": "Valeur dans la norme" } ]
    }
  ]
}
```

## Réponses

| Code HTTP | Condition                                              |
|-----------|--------------------------------------------------------|
| `201 Created`       | Import réussi                                |
| `400 Bad Request`   | JSON malformé, `resourceType` incorrect, résultats vides, `subject.reference` manquant |
| `401 Unauthorized`  | Clé API manquante ou incorrecte              |
| `500 Internal Server Error` | Erreur inattendue du serveur           |

### Corps 400

```json
{
  "status": 400,
  "error": "Bad Request",
  "message": "Format FHIR DiagnosticReport invalide : aucun résultat trouvé dans 'result'."
}
```

## Exemple cURL

```bash
curl -X POST https://api.joprelys.com/api/public/lab-integration/fhir/diagnostic-report \
  -H "Content-Type: application/json" \
  -H "X-API-KEY: lab-partner-secret-token" \
  -d '{
    "fhirJson": "{\"resourceType\":\"DiagnosticReport\",\"subject\":{\"reference\":\"ServiceRequest/REQ-2024-001\"},\"effectiveDateTime\":\"2024-06-15T08:00:00Z\",\"issued\":\"2024-06-15T10:30:00Z\",\"result\":[{\"code\":{\"text\":\"Hémoglobine\"},\"valueQuantity\":{\"value\":14.2,\"unit\":\"g/dL\"}}]}",
    "validatorName": "Dr. Martin"
  }'
```

## Notes FHIR R4

- Seul le format **JSON** est supporté (pas XML)
- Profil : **FHIR R4** uniquement (pas R3/R5)
- `subject.reference` doit contenir une référence de type `ServiceRequest/{id}` ou `{id}`
- Les `Observation` inline (embedded dans `result`) sont supportées ; les références externes ne le sont pas
