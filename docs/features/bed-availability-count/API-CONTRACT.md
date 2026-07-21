# Contrat API — Compteur des lits disponibles

## Endpoint concerné

```http
GET /api/spatial/wards/{wardId}/occupancy
Authorization: Bearer <token>
Permission: HOSPITALIZATION_READ
```

## Réponse enrichie

```json
{
  "id": "uuid",
  "name": "Médecine",
  "rooms": [],
  "totalBedsCount": 4,
  "occupiedBedsCount": 1,
  "availableBedsCount": 1
}
```

## Sémantique

| Champ | Définition |
|---|---|
| `totalBedsCount` | tous les lits configurés dans le service |
| `occupiedBedsCount` | lits dont le statut est `OCCUPIED` |
| `availableBedsCount` | lits dont le statut est exactement `FREE` |

`CLEANING` et `MAINTENANCE` ne contribuent pas à `availableBedsCount`.

## Compatibilité

Le changement est additif. Les champs existants, les statuts HTTP et les permissions ne changent pas. Ordre de déploiement recommandé : backend puis Angular.
