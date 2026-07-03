# STORY-1104 — Import FHIR DiagnosticReport : Spécification Fonctionnelle

## Contexte

Les laboratoires partenaires de Joprelys Connect utilisent le standard **FHIR R4** pour échanger leurs
résultats biologiques. Afin d'éviter toute ré-implémentation manuelle pour chaque partenaire, la
plateforme doit accepter nativement un document `DiagnosticReport` FHIR et l'ingérer dans le système
existant de résultats de laboratoire.

## Objectif

Permettre à un laboratoire externe d'envoyer ses résultats au format **FHIR R4 JSON DiagnosticReport**
via une API REST sécurisée par clé API, sans devoir mapper manuellement chaque champ vers le format
interne.

## Acteurs

| Acteur                  | Rôle                                                              |
|-------------------------|-------------------------------------------------------------------|
| Laboratoire externe     | Émet le `DiagnosticReport` FHIR JSON via HTTP POST               |
| API Joprelys Connect    | Reçoit, valide, parse et stocke le rapport biologique            |
| `FhirDiagnosticReportParser` | Traduit le format FHIR vers le format interne `LabResultUploadRequest` |

## Scénarios fonctionnels

### SC-01 — Import réussi d'un rapport FHIR valide
- **Donné** un laboratoire partenaire possédant une clé API valide
- **Quand** il envoie un `DiagnosticReport` FHIR R4 JSON contenant au moins un analyte
- **Alors** le système retourne HTTP 201 et le rapport est enregistré avec le numéro de demande
  extrait de `subject.reference`

### SC-02 — Clé API manquante ou invalide
- **Donné** une requête sans en-tête `X-API-KEY` ou avec une valeur erronée
- **Alors** le système retourne HTTP 401 sans traiter le payload

### SC-03 — Payload FHIR avec `resourceType` incorrect
- **Donné** un JSON dont `resourceType` n'est pas `DiagnosticReport`
- **Alors** le système retourne HTTP 400 avec un message explicite

### SC-04 — Payload FHIR sans analytes (`result: []`)
- **Donné** un `DiagnosticReport` dont le tableau `result` est vide
- **Alors** le système retourne HTTP 400 : *"aucun résultat trouvé dans 'result'"*

### SC-05 — JSON malformé
- **Donné** un corps de requête qui n'est pas du JSON valide
- **Alors** le système retourne HTTP 400

### SC-06 — `validatorName` absent → valeur par défaut
- **Donné** un payload FHIR valide sans `validatorName`
- **Alors** le système utilise `"Système FHIR"` comme nom du validateur

## Contraintes métier

- Seul le format **FHIR R4 JSON** est supporté (pas XML, pas FHIR R3/R5)
- Le champ `subject.reference` est obligatoire et sert de `examRequestNumber`
- Le `pdfBase64` et le `sampleCollectedAt` ne sont pas portés par FHIR DiagnosticReport → `null`
- Aucune bibliothèque HAPI FHIR n'est utilisée (parsing Jackson natif)
