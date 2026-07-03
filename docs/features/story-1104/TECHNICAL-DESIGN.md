# STORY-1104 — Import FHIR DiagnosticReport : Conception Technique

## Architecture

```
POST /api/public/lab-integration/fhir/diagnostic-report
  │
  ▼
LabResultUploadController          ← API layer (délègue, pas de logique)
  │  valide X-API-KEY
  │  injecte FhirDiagnosticReportParser
  ▼
FhirDiagnosticReportParser         ← Application layer (parsing Jackson)
  │  parse(fhirJson, validatorName)
  │  → LabResultUploadRequest
  ▼
LabResultService                   ← Domain service existant
  │  uploadResults(request)
  ▼
[persistance existante]
```

## Composants créés

| Fichier | Package | Rôle |
|---------|---------|------|
| `FhirDiagnosticReportParser.java` | `...lab.application` | Parseur Jackson FHIR R4 |
| `FhirDiagnosticReportUploadRequest.java` | `...lab.api` | DTO requête entrant |
| `LabResultUploadController.java` | `...lab.api` | Endpoint ajouté (modifié) |
| `FhirDiagnosticReportParserTest.java` | `...lab` (test) | Tests unitaires |

## Mapping FHIR → LabResultUploadRequest

| Champ `LabResultUploadRequest` | Source FHIR R4                          |
|--------------------------------|-----------------------------------------|
| `examRequestNumber`            | `subject.reference` (partie après `/`)  |
| `validatorName`                | `validatorName` du DTO (ou `"Système FHIR"`) |
| `sampleCollectedAt`            | `null` (non standard)                   |
| `resultAt`                     | `effectiveDateTime`                     |
| `validatedAt`                  | `issued`                                |
| `conclusion`                   | `null` (non mappé)                      |
| `results[].analyteName`        | `result[].code.text`                    |
| `results[].value`              | `result[].valueQuantity.value`          |
| `results[].unit`               | `result[].valueQuantity.unit`           |
| `results[].referenceRange`     | `result[].referenceRange[0].text`       |
| `results[].interpretation`     | `result[].interpretation[0].text`       |
| `results[].comment`            | `result[].note[0].text`                 |
| `pdfBase64`                    | `null`                                  |

## Décisions d'architecture

### Pas de HAPI FHIR
Utilisation de Jackson natif (`ObjectMapper`) pour éviter une dépendance lourde (HAPI FHIR ~60 MB).
Le profil FHIR R4 JSON DiagnosticReport est suffisamment simple pour être traité avec `JsonNode`.

### Pattern de gestion d'erreurs
Toutes les erreurs de parsing lèvent `ResponseStatusException(400)` directement depuis le parseur.
Le controller ne gère pas les exceptions : Spring `ResponseEntityExceptionHandler` les intercepte.

### Valeur par défaut `validatorName`
Si absent ou blank → `"Système FHIR"` pour garantir la traçabilité.

### Sécurité
Identique à l'endpoint `/upload` existant : validation `X-API-KEY` via `@Value`
(`${joprelys.lab-integration.api-key:lab-partner-secret-token}`).

## Contraintes de qualité

- Aucune méthode > 40 lignes ✓
- Classe < 500 lignes ✓ (`FhirDiagnosticReportParser` : ~160 lignes)
- SOLID : controller délègue au service, parseur est un composant applicatif distinct ✓
