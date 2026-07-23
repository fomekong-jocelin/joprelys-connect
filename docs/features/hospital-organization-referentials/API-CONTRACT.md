# API-CONTRACT — HOS-ORG-001-A

Base path : `/api/hospital-organization`

## Catalogues

### `GET /catalogs/services`

Retourne les entrées actives du catalogue service :

```json
[
  {
    "code": "GENERAL_MEDICINE",
    "nameFr": "Médecine générale",
    "nameEn": "General medicine",
    "serviceType": "OUTPATIENT"
  }
]
```

### `GET /catalogs/specialties`

Retourne les spécialités actives :

```json
[
  {
    "code": "GENERAL_MEDICINE",
    "nameFr": "Médecine générale",
    "nameEn": "General medicine"
  }
]
```

## Unités organisationnelles

### `GET /units?includeInactive=false`

Retourne les unités du tenant courant, triées de manière stable. La réponse contient `parentId` pour permettre au frontend de construire l'arbre sans dupliquer les règles métier.

```json
[
  {
    "id": "uuid",
    "code": "SVC-GEN-MED",
    "name": "Médecine générale",
    "unitType": "SERVICE",
    "serviceCatalogCode": "GENERAL_MEDICINE",
    "parentId": null,
    "active": true
  }
]
```

### `POST /units`

Permission : `ORGANIZATION_STRUCTURE_MANAGE`.

```json
{
  "code": "SVC-GEN-MED",
  "unitType": "SERVICE",
  "parentId": null,
  "serviceCatalogCode": "GENERAL_MEDICINE",
  "name": null
}
```

Pour `SERVICE`, `name` est ignoré/refusé s'il est fourni et le backend dérive le nom du catalogue. Pour `POLE`, `DEPARTMENT`, `CARE_UNIT`, `name` est obligatoire et `serviceCatalogCode` doit être null.

Réponses : `201`, `400`, `403`, `409`.

### `PUT /units/{id}`

Modifie `code`, `parentId` et, selon le type, `name` ou `serviceCatalogCode`. Un changement de `unitType` n'est pas autorisé dans ce lot : recréer un objet métier différent serait une opération structurante séparée.

Réponses : `200`, `400`, `403`, `404`, `409`.

### `POST /units/{id}/deactivate`

Désactive l'unité si elle n'a aucun enfant actif. Les références historiques restent intactes.

### `POST /units/{id}/activate`

Réactive l'unité si son parent éventuel est actif et si son catalogue éventuel est actif.

## Sécurité

- tenant dérivé de l'utilisateur authentifié, jamais fourni dans le payload ;
- aucune mutation cross-tenant ;
- API maître de toutes les validations hiérarchiques ;
- pas d'exposition d'entités JPA.
