# Contrat API — Gestion du Personnel Clinique (STORY-0104)

## 1. Sécurité

Tous les endpoints sont sous `/api/staff` et nécessitent un JWT valide avec le rôle `ADMIN_CLINIQUE`.

Règles communes :
- l'administrateur agit uniquement sur son `organizationId` ;
- les rôles gérables sont `MEDECIN`, `INFIRMIER`, `AGENT_ACCUEIL`, `PHARMACIEN` ;
- un administrateur ne peut pas gérer son propre compte via cette API ;
- les erreurs sont renvoyées au format `ProblemDetail`.

## 2. Lister le personnel

```http
GET /api/staff
Authorization: Bearer <token>
```

Réponse `200 OK` :

```json
[
  {
    "id": "7c76cda7-7a10-46e8-bb4c-9ed454fa08fc",
    "email": "medecin.a@joprelys.local",
    "displayName": "Dr Alpha",
    "role": "MEDECIN",
    "enabled": true,
    "createdAt": "2026-07-02T10:00:00Z"
  }
]
```

## 3. Inviter un collaborateur

```http
POST /api/staff
Authorization: Bearer <token>
Content-Type: application/json
```

Payload :

```json
{
  "email": "nouveau.medecin@joprelys.local",
  "displayName": "Dr Nouveau",
  "role": "MEDECIN"
}
```

Réponse `201 Created` :

```json
{
  "id": "c76cda74-7a10-46e8-bb4c-9ed454fa08fc",
  "email": "nouveau.medecin@joprelys.local",
  "displayName": "Dr Nouveau",
  "role": "MEDECIN",
  "enabled": true,
  "temporaryPassword": "Jop-F3B8G9",
  "createdAt": "2026-07-02T10:00:00Z"
}
```

Erreurs :
- `400 Bad Request` si l'email existe déjà ;
- `400 Bad Request` si le rôle n'est pas un rôle clinique gérable ;
- `403 Forbidden` si l'administrateur n'est pas rattaché à une clinique.

## 4. Modifier un collaborateur

```http
PUT /api/staff/{id}
Authorization: Bearer <token>
Content-Type: application/json
```

Payload :

```json
{
  "displayName": "Dr Alpha Senior",
  "role": "PHARMACIEN"
}
```

Réponse `200 OK` : `StaffResponse`.

Erreurs :
- `404 Not Found` si le collaborateur n'appartient pas à la clinique de l'administrateur ;
- `400 Bad Request` si le rôle n'est pas autorisé.

## 5. Activer ou désactiver un collaborateur

```http
POST /api/staff/{id}/toggle
Authorization: Bearer <token>
```

Réponse `200 OK` : `StaffResponse` avec `enabled` inversé.

Un compte désactivé reste en base mais ne peut plus se connecter via `/api/auth/login`.
