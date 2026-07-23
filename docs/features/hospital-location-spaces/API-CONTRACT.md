# API-CONTRACT — HOS-LOC-001-A

## 1. Principes

- aucun payload ne transporte `organizationId` comme donnée métier ; le scope vient de l'authentification ou du paramètre plateforme déjà utilisé par les API de configuration ;
- aucune clé métier n'est résolue par nom ;
- les UUID structurés sont sources de vérité ;
- les libellés sont des valeurs d'affichage/snapshot ;
- les mutations de configuration exigent `SPATIAL_CONFIGURATION_MANAGE`.

## 2. Configuration géographique

Base : `/api/spatial/configuration`.

### `GET /location-types`

```json
[
  { "code": "SITE", "rank": 10 },
  { "code": "BUILDING", "rank": 20 },
  { "code": "FLOOR", "rank": 30 },
  { "code": "ZONE", "rank": 40 }
]
```

### `GET /locations?includeInactive=false`

```json
[
  {
    "id": "uuid",
    "parentId": null,
    "code": "BLD_MAIN",
    "name": "Bâtiment principal",
    "nodeType": "BUILDING",
    "active": true
  }
]
```

### `POST /locations`

```json
{
  "parentId": null,
  "code": "BLD_MAIN",
  "name": "Bâtiment principal",
  "nodeType": "BUILDING"
}
```

Réponses : `201`, `400`, `403`, `404`, `409`.

### `PUT /locations/{id}`

Le `nodeType` n'est pas modifiable. Un changement de type exige un nouvel objet ; le code, nom et parent peuvent être corrigés selon les contraintes.

### `POST /locations/{id}/activate`
### `POST /locations/{id}/deactivate`

Pas de DELETE physique public.

## 3. Types et espaces

### `GET /space-types`

```json
[
  {
    "code": "HOSPITAL_ROOM",
    "nameFr": "Chambre d'hospitalisation",
    "nameEn": "Hospital room",
    "inpatientCompatible": true
  }
]
```

Le champ `inpatientCompatible` est une projection calculée à partir du sous-référentiel, pas une vérité dupliquée modifiable par le client.

### `GET /spaces?includeInactive=false&locationNodeId={uuid?}`

```json
[
  {
    "id": "uuid",
    "locationNodeId": "uuid",
    "code": "ROOM_201",
    "name": "Chambre 201",
    "spaceTypeCode": "HOSPITAL_ROOM",
    "inpatientProfile": true,
    "active": true
  }
]
```

### `POST /spaces`

```json
{
  "locationNodeId": "uuid-or-null",
  "code": "ROOM_201",
  "name": "Chambre 201",
  "spaceTypeCode": "HOSPITAL_ROOM",
  "enableInpatientProfile": true
}
```

Règles :

- `enableInpatientProfile=true` seulement pour un type autorisé ;
- aucun profil implicite pour un type non compatible ;
- un espace direct sous établissement utilise `locationNodeId=null`.

### `PUT /spaces/{id}`

Autorise correction code/nom/localisation. Le changement de `spaceTypeCode` est refusé dès qu'un profil/lit/historique rendrait l'opération ambiguë ; sinon il suit les contraintes documentées.

### `POST /spaces/{id}/activate`
### `POST /spaces/{id}/deactivate`

### `POST /spaces/{id}/inpatient-profile`

Crée le profil si type compatible et espace actif.

### `DELETE /spaces/{id}/inpatient-profile`

Autorisé uniquement s'il n'existe aucun lit ou historique dépendant. Sinon `409`.

## 4. Rattachements unité ↔ espace

### `GET /unit-space-assignments?spaceId={uuid?}&unitId={uuid?}&activeAt={instant?}`

### `POST /unit-space-assignments`

```json
{
  "organizationalUnitId": "uuid",
  "spaceId": "uuid",
  "validFrom": "2026-07-23T12:00:00Z",
  "validTo": null
}
```

Réponses :

- `201` création ;
- `400` période invalide ;
- `403` permission ;
- `404` objet cross-tenant/introuvable ;
- `409` chevauchement du même couple.

### `PUT /unit-space-assignments/{id}`

Permet de corriger/clôturer une période sans supprimer l'historique. Une période déjà impliquée dans un fait métier ne doit pas être réécrite pour masquer le passé ; les contrôles d'immutabilité sont appliqués par le service selon les références existantes.

## 5. Lits — configuration

### `POST /beds`

Nouveau payload :

```json
{
  "spaceId": "uuid",
  "bedNumber": "A"
}
```

Il n'existe plus de `roomId`.

### `PUT /beds/{id}`

```json
{
  "spaceId": "uuid",
  "bedNumber": "A"
}
```

Le déplacement d'un lit ayant un historique d'affectation doit être refusé ou traité par un workflow ultérieur ; aucune réécriture silencieuse du passé.

Les endpoints d'état existants `/api/spatial/beds/{id}/...` sont conservés, car ils opèrent déjà par UUID et sont indépendants du modèle Ward/Room.

## 6. Admission hospitalière — BREAKING

L'ancien contrat :

```json
{
  "serviceName": "Médecine générale",
  "roomNumber": "201",
  "bedNumber": "A"
}
```

est supprimé.

Nouveau contrat :

```json
{
  "patientId": "uuid",
  "serviceUnitId": "uuid",
  "spaceId": "uuid",
  "bedId": "uuid",
  "admissionReason": "Surveillance clinique",
  "visitId": "uuid-or-null",
  "emergencyId": "uuid-or-null",
  "responsiblePractitionerId": "uuid"
}
```

Règles backend :

- visitId ou emergencyId obligatoire ;
- service unit actif dans le tenant ;
- space actif dans le tenant ;
- assignment unité-space actif au moment de l'admission ;
- space possède un inpatient profile ;
- bed appartient exactement au space ;
- bed ouvert/prêt/non affecté ;
- snapshots dérivés côté backend.

Aucun constructeur ou DTO backward-compatible n'est conservé.

## 7. Hospitalization response

Réponse structurée :

```json
{
  "id": "uuid",
  "patientId": "uuid",
  "currentServiceUnitId": "uuid",
  "currentSpaceId": "uuid",
  "currentBedId": "uuid",
  "serviceName": "Médecine générale",
  "spaceName": "Chambre 201",
  "bedNumber": "A",
  "status": "EN_COURS"
}
```

`serviceName`, `spaceName`, `bedNumber` sont explicitement des snapshots d'affichage ; les champs `current*Id` sont les références courantes.

## 8. Transfert — BREAKING

Ancien :

```json
{
  "hospitalizationId": "uuid",
  "newBedId": "uuid"
}
```

Nouveau :

```json
{
  "hospitalizationId": "uuid",
  "targetServiceUnitId": "uuid",
  "targetSpaceId": "uuid",
  "targetBedId": "uuid"
}
```

Motif : un espace/lit peut être utilisé par plusieurs unités ; le service métier cible ne peut pas être deviné depuis le lit.

## 9. Lecture capacité

Les endpoints Ward sont supprimés et remplacés par des projections UUID :

### `GET /api/spatial/spaces/{spaceId}/occupancy`

Retourne : espace, lits, installed/open/ready/occupied/available.

### `GET /api/spatial/organizational-units/{unitId}/occupancy`

Agrège les espaces dont le rattachement est actif à l'instant de la requête. Un espace partagé peut apparaître dans plusieurs vues d'unité ; les agrégats direction multi-unité devront éviter le double comptage dans HOS-KPI.

## 10. Endpoints supprimés

Après migration de tous les consommateurs :

- `/api/spatial/wards`
- `/api/spatial/wards/{id}/occupancy`
- `/api/spatial/configuration/wards...`
- `/api/spatial/configuration/rooms...`

Aucun alias de compatibilité n'est créé.
