# Services hospitaliers typés — Contrat API

## Rupture de contrat

Le champ `serviceType` est obligatoire. L'ancien payload `{ "name": "..." }` n'est plus accepté.

## Valeurs autorisées

```text
HOSPITALIZATION
EMERGENCY
OUTPATIENT
MEDICO_TECHNICAL
PHARMACY
ADMINISTRATIVE
```

## Créer un service

`POST /api/spatial/configuration/wards`

### Requête

```json
{
  "name": "Caisse principale",
  "serviceType": "ADMINISTRATIVE"
}
```

### Réponse `201`

```json
{
  "id": "uuid",
  "name": "Caisse principale",
  "serviceType": "ADMINISTRATIVE",
  "allowsRooms": false
}
```

### Erreurs

- `400` : type absent ou valeur inconnue ;
- `409` : nom déjà utilisé dans la clinique.

## Modifier un service

`PUT /api/spatial/configuration/wards/{id}`

### Requête

```json
{
  "name": "Médecine interne",
  "serviceType": "HOSPITALIZATION"
}
```

### Réponse `200`

Même schéma que la création.

### Erreurs

- `404` : service introuvable ;
- `409` : nom dupliqué ;
- `409` : passage vers un type sans chambres alors que des chambres existent.

## Lire la configuration

`GET /api/spatial/configuration`

```json
{
  "wards": [
    {
      "id": "uuid",
      "name": "Médecine interne",
      "serviceType": "HOSPITALIZATION",
      "allowsRooms": true,
      "rooms": []
    },
    {
      "id": "uuid",
      "name": "Caisse principale",
      "serviceType": "ADMINISTRATIVE",
      "allowsRooms": false,
      "rooms": []
    }
  ]
}
```

`rooms` reste présent pour un schéma stable de lecture, mais il est obligatoirement vide lorsque `allowsRooms=false`.

## Créer une chambre

`POST /api/spatial/configuration/rooms`

Le contrat de requête reste identique. La règle supplémentaire est exécutée côté backend : le service cible doit autoriser les chambres.

### Erreur métier

`409 Conflict`

```json
{
  "detail": "Le type de ce service n'autorise pas la création de chambres."
}
```

## Admission

Le contrat d'admission ne crée plus de structure. Lorsque le triplet service/chambre/lit n'existe pas :

`404 Not Found`

```json
{
  "detail": "Le lit sélectionné n'existe pas dans la structure configurée."
}
```

## Sécurité

- gestion : `SPATIAL_CONFIGURATION_MANAGE` ;
- lecture opérationnelle : permissions existantes d'hospitalisation ;
- `organizationId` optionnel uniquement pour un administrateur plateforme ;
- aucune modification des règles tenant.
