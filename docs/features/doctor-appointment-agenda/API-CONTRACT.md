# API-CONTRACT — Agenda personnel du médecin

## GET `/api/doctor/appointments`

Retourne les rendez-vous du médecin authentifié pour une période donnée.

### Autorisation

```text
APPOINTMENT_READ_OWN
```

Cette permission est attribuée uniquement au rôle système `MEDECIN`. L’API n’accepte jamais de `doctorId` : l’identité est extraite du JWT.

### Paramètres

| Paramètre | Type | Obligatoire | Règle |
|---|---|---:|---|
| `from` | ISO-8601 Instant | oui | inclusif |
| `to` | ISO-8601 Instant | oui | exclusif, strictement après `from` |

La période ne peut pas dépasser 92 jours.

### Réponse `200`

```json
[
  {
    "id": "7b4ad7f5-b265-41f1-a7ea-3ff706247cf3",
    "patientId": "3df5fc8e-4a3d-4fbf-a5b7-ad99de210a64",
    "patientDisplayName": "Patient Alice",
    "patientLocalNumber": "PAT-00042",
    "startAt": "2026-07-20T08:00:00Z",
    "endAt": "2026-07-20T08:30:00Z",
    "status": "CONFIRMED",
    "reason": "Suivi cardiologique",
    "visitId": null
  }
]
```

### Erreurs

#### `400 VALIDATION_ERROR`

```json
{
  "error": {
    "code": "VALIDATION_ERROR",
    "message": "La période demandée est invalide.",
    "trace_id": "trc_..."
  }
}
```

Cas :

- paramètre absent ou mal formé ;
- `to <= from` ;
- période supérieure à 92 jours.

#### `401`

- utilisateur non authentifié ;
- token invalide ou session révoquée ;
- compte utilisateur désactivé ou identité RBAC non résolue.

#### `403`

- permission `APPOINTMENT_READ_OWN` absente ;
- permission accordée par erreur à une identité résolue qui n’est pas un médecin.

Dans ce dernier cas, le code métier est `DOCTOR_APPOINTMENT_ACCESS_DENIED`.

### Confidentialité

Le contrat n’expose pas : téléphone, e-mail, adresse, date de naissance, sexe, données cliniques, allergies, antécédents, informations d’urgence ou données financières.
