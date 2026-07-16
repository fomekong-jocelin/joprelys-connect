# Contrat API — Structure hospitalière

Base : `/api/spatial/configuration`

Tous les endpoints nécessitent un rôle administrateur autorisé.

Le paramètre optionnel `organizationId` peut être omis par un `ADMIN_CLINIQUE`, dont la clinique de rattachement est imposée, et devient obligatoire pour `ADMIN_JOPRELYS`/`SUPER_ADMIN`. Le backend vérifie ce périmètre avant d'ouvrir la transaction tenantée.

## Lecture

- `GET /` — retourne la structure complète de la clinique courante.

## Services

- `POST /wards` — `{ "name": "Cardiologie" }`
- `PUT /wards/{id}` — `{ "name": "Cardiologie interventionnelle" }`
- `DELETE /wards/{id}` — `204`

## Chambres

- `POST /rooms` — `{ "wardId": "uuid", "roomNumber": "101", "capacity": 2, "comfortLevel": "STANDARD" }`
- `PUT /rooms/{id}` — même payload.
- `DELETE /rooms/{id}` — `204`

## Lits

- `POST /beds` — `{ "roomId": "uuid", "bedNumber": "101-A" }`
- `PUT /beds/{id}` — même payload.
- `DELETE /beds/{id}` — `204`

## Erreurs

- `400` : données invalides.
- `403` : rôle non autorisé.
- `404` : ressource inexistante dans le tenant courant.
- `409` : doublon, capacité dépassée ou suppression interdite.
