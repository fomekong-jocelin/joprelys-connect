# TECHNICAL-DESIGN — Gestion Spatiale (Lits & Chambres)

## 1. Objectif technique

Concevoir et implémenter la structure de persistance et les APIs permettant de gérer les services (Wards), chambres (Rooms) et lits (Beds), d'attribuer transactionnellement un lit libre à un patient hospitalisé, et de gérer les transferts de lits en prévenant les accès concurrents.

## 2. Stack concernée

- [x] Spring Boot (JPA, REST API)
- [x] Angular (Tailwind CSS v4, dynamic grid)
- [x] Base de données (Flyway migrations, PostgreSQL/H2)
- [x] Documentation functional & technical

## 3. Architecture cible

Le service de spatialisation s'intégrera dans le package `com.joprelys.backend.hospitalization` ou dans un nouveau package `com.joprelys.backend.spatial`. Compte tenu de la forte dépendance avec les hospitalisations, nous le placerons dans `com.joprelys.backend.spatial` pour conserver des modules à haute cohésion.

```text
SpatialController (REST API)
  → SpatialService (Orchestration métier)
    → WardEntity / RoomEntity / BedEntity (Modèle de domaine JPA)
    → BedAssignmentEntity (Historique des transferts)
  ← WardRepository / RoomRepository / BedRepository (Persistence)
```

## 4. Modèle de données / migrations

### Migration SQL Flyway (`V44__create_spatial_tables.sql`)

```sql
CREATE TABLE wards (
    id UUID PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    organization_id UUID NOT NULL,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL
);

CREATE TABLE rooms (
    id UUID PRIMARY KEY,
    ward_id UUID NOT NULL REFERENCES wards(id) ON DELETE CASCADE,
    room_number VARCHAR(20) NOT NULL,
    capacity INT NOT NULL,
    comfort_level VARCHAR(50) NOT NULL, -- VIP, STANDARD
    organization_id UUID NOT NULL,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL
);

CREATE TABLE beds (
    id UUID PRIMARY KEY,
    room_id UUID NOT NULL REFERENCES rooms(id) ON DELETE CASCADE,
    bed_number VARCHAR(20) NOT NULL,
    status VARCHAR(50) NOT NULL DEFAULT 'FREE', -- FREE, OCCUPIED, CLEANING, MAINTENANCE
    organization_id UUID NOT NULL,
    version INT NOT NULL DEFAULT 0, -- Pour le verrouillage optimiste JPA
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL
);

CREATE TABLE bed_assignments (
    id UUID PRIMARY KEY,
    hospitalization_id UUID NOT NULL, -- Lié à l'hospitalisation active
    bed_id UUID NOT NULL REFERENCES beds(id),
    assigned_at TIMESTAMP NOT NULL,
    released_at TIMESTAMP,
    organization_id UUID NOT NULL
);
```

### Verrouillage Optimiste JPA (Optimistic Locking)

L'entité `BedEntity` inclura :
```java
@Version
private Integer version;
```
Toute mise à jour du statut du lit (passage de `FREE` à `OCCUPIED` au moment de la transaction d'hospitalisation) vérifiera ce numéro de version. Si deux soignants tentent d'affecter le même lit en même temps, le second recevra une `ObjectOptimisticLockingFailureException`, qui sera capturée par le `GlobalExceptionHandler` pour renvoyer une erreur `409 Conflict`.

## 5. Contrats API

| Méthode | Endpoint | Request | Response | Erreurs / Statuts |
|---|---|---|---|---|
| `GET` | `/api/spatial/wards` | N/A | `List<WardResponse>` | `200 OK` |
| `GET` | `/api/spatial/wards/{id}/occupancy` | N/A | `WardOccupancyResponse` | `200 OK`, `404 Not Found` |
| `POST` | `/api/spatial/beds/{id}/status` | `{ "status": "CLEANING" }` | `BedResponse` | `200 OK`, `400 Bad Request` |
| `POST` | `/api/spatial/transfers` | `{ "hospitalizationId": "...", "newBedId": "..." }` | `BedAssignmentResponse` | `200 OK`, `409 Conflict` (Lit déjà pris) |

## 6. Sécurité

- Accès restreint aux rôles soignants : `INFIRMIER`, `MEDECIN`, `ADMIN_CLINIQUE`.
- Multi-tenancy assuré par l'annotation `@TenantId` sur les entités JPA, garantissant que les utilisateurs d'une clinique ne peuvent pas voir ou modifier les lits d'une autre clinique.

## 7. Tests prévus

- **Unit/Integration Tests (Backend)** :
  - Test de concurrence : simuler 2 threads réservant simultanément le même lit et vérifier que l'une des requêtes échoue proprement avec un `ObjectOptimisticLockingFailureException`.
  - Test du cycle de vie des lits (libération de lit ➔ statut `CLEANING`).
- **Vitest (Frontend)** :
  - Vérification de l'affichage de la grille réactive de l'occupation des lits.

## 8. Impact version / SemVer

- **MINOR bump** : Ajout d'une nouvelle fonctionnalité majeure rétrocompatible.
