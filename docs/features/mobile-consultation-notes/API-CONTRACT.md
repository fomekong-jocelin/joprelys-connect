# Contrat API — Note clinique SOAP unifiée (MOB-2821)

> Source de vérité proposée par ADR-0005. Le même contrat est consommé par
> Angular et Flutter.

## Sémantique SOAP

| Section | Champs API |
|---|---|
| S — Subjectif | `symptoms` |
| O — Objectif | `clinicalExam` |
| A — Évaluation | `diagnosis` |
| P — Plan | `conclusion`, `advice`, `followUp` |

`diagnosis` est l'unique diagnostic obligatoire documenté par le praticien.
Le contrat ne qualifie pas artificiellement cette valeur comme hypothèse,
diagnostic principal ou diagnostic final en l'absence de workflow métier dédié.

## Lire une consultation

- **GET** `/api/visits/{id}/consultation`
- Permission : `CLINICAL_READ`
- `200 OK` : consultation détaillée.
- `204 No Content` : visite valide sans consultation.
- `403 Forbidden` : permission ou périmètre clinique insuffisant.
- `404 Not Found` : visite introuvable.

## Enregistrer une consultation

- **POST** `/api/visits/{id}/consultation`
- Permission : `CLINICAL_WRITE`
- Visite requise au statut `EN_COURS`.

```json
{
  "symptoms": "Douleurs abdominales intenses depuis deux heures.",
  "clinicalExam": "Abdomen souple, douleur de la fosse iliaque droite.",
  "diagnosis": "Appendicite aiguë suspectée au vu du tableau clinique.",
  "conclusion": "Tableau compatible avec une urgence chirurgicale.",
  "advice": "Rester à jeun et consulter immédiatement en cas d'aggravation.",
  "followUp": "Avis chirurgical et réévaluation après imagerie."
}
```

- `200 OK` : consultation enregistrée.
- `400 Bad Request` : validation ou visite non active.
- `403 Forbidden` : permission ou périmètre clinique insuffisant.
- `404 Not Found` : visite ou praticien introuvable.

## Données associées

Les constantes, prescriptions et demandes d'examens restent structurées dans
leurs contrats dédiés. Elles peuvent être affichées dans le parcours SOAP, mais
ne doivent pas être dupliquées dans un champ `plan` libre comme seule source de
vérité.
