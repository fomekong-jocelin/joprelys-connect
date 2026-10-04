# API-CONTRACT — HOS-ORG-001-A

Base path : `/api/hospital-organization`

Toutes les routes de ce lot nécessitent `ORGANIZATION_STRUCTURE_MANAGE`. Un administrateur plateforme fournit explicitement `organizationId`; pour un administrateur d'établissement, le tenant est dérivé du compte authentifié.

## Catalogues

### `GET /catalogs/services`

Retourne les entrées actives du catalogue service :

```json
[
  {
    "code": "GENERAL_MEDICINE",
    "nameFr": "Médecine générale",
    "nameEn": "General medicine"
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

Retourne les unités du tenant courant, triées de manière stable. La réponse contient `parentId` afin que le frontend construise l'arbre sans dupliquer les règles métier.

Depuis V113, une unité `SERVICE` peut porter un nom local optionnel de 2 à 120 caractères après trim. Le libellé visible utilise ce nom s'il existe ; sinon il est résolu depuis `serviceCatalogCode` et le catalogue FR/EN selon la locale active. Les anciens `name: null` restent acceptés.

```json
[
  {
    "id": "uuid",
    "code": "SVC_GEN_MED",
    "name": null,
    "unitType": "SERVICE",
    "serviceCatalogCode": "GENERAL_MEDICINE",
    "parentId": null,
    "active": true
  }
]
```

Pour `POLE`, `DEPARTMENT` et `CARE_UNIT`, `name` contient le nom propre choisi par l'établissement.

### `POST /units`

```json
{
  "code": "SVC_GEN_MED",
  "unitType": "SERVICE",
  "parentId": null,
  "serviceCatalogCode": "GENERAL_MEDICINE",
  "name": null
}
```

Pour `SERVICE`, `serviceCatalogCode` reste obligatoire et `name` est optionnel ; un nom vide devient null et un nom local non vide est validé/persisté. Pour `POLE`, `DEPARTMENT`, `CARE_UNIT`, `name` est obligatoire et `serviceCatalogCode` doit être null.

Réponses : `201`, `400`, `403`, `409`.

### `PUT /units/{id}`

Modifie `code`, `parentId` et, selon le type, `name` ou `serviceCatalogCode`. Un changement de `unitType` n'est pas autorisé dans ce lot.

Réponses : `200`, `400`, `403`, `404`, `409`.

### `POST /units/{id}/deactivate`

Désactive l'unité si elle n'a aucun enfant actif. Les références historiques restent intactes.

### `POST /units/{id}/activate`

Réactive l'unité si son parent éventuel est actif et si son catalogue éventuel est actif.

## Sécurité

- tenant dérivé de l'utilisateur authentifié ou du scope plateforme autorisé ;
- `organizationId` n'est jamais accepté dans le body métier ;
- aucune mutation cross-tenant ;
- requêtes repository explicitement filtrées par `organizationId` en complément de `@TenantId` ;
- FK composite empêchant un parent d'un autre tenant ;
- API maître de toutes les validations hiérarchiques ;
- pas d'exposition d'entités JPA.
