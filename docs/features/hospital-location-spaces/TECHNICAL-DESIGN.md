# TECHNICAL-DESIGN — HOS-LOC-001-A

## 1. Architecture cible

Le contexte `spatial` est conservé pour les notions physiques et de capacité, mais son modèle interne est remplacé :

```text
Hospital Organization (V87)
OrganizationalUnit
        │
        │ dated N:N
        ▼
FacilitySpace ◄──────── FacilityLocationNode
        │
        │ 1:0..1 inpatient profile
        ▼
InpatientSpaceProfile
        │
        ▼
Bed
```

`WardEntity` et `RoomEntity` disparaissent lorsque tous leurs consommateurs ont été migrés dans la même PR.

## 2. Entités

### FacilityLocationNode

- `id UUID` ;
- `organizationId UUID` ;
- `parentId UUID nullable` ;
- `code varchar(64)` ;
- `name varchar(120)` ;
- `nodeType` : SITE / BUILDING / FLOOR / ZONE ;
- `active` ;
- timestamps.

Validation backend :

| Parent | Enfants autorisés |
|---|---|
| racine | SITE, BUILDING, FLOOR, ZONE |
| SITE | BUILDING, FLOOR, ZONE |
| BUILDING | FLOOR, ZONE |
| FLOOR | ZONE |
| ZONE | aucun nœud |

La validation autorise volontairement les niveaux sautés.

### SpaceTypeCatalogEntry

Catalogue global read-only :

- `code` ;
- `nameFr` ;
- `nameEn` ;
- `active`.

La compatibilité avec les lits n'est pas un booléen copié dans chaque espace : elle est portée par un sous-référentiel `inpatient_space_type_catalog` afin de pouvoir la garantir par FK.

### FacilitySpace

- `id UUID` ;
- `organizationId UUID` ;
- `locationNodeId UUID nullable` ;
- `code varchar(64)` ;
- `name varchar(120)` ;
- `spaceTypeCode varchar(64)` ;
- `active` ;
- timestamps.

Un espace peut exister directement sous l'établissement (`locationNodeId = null`).

### InpatientSpaceProfile

Extension 1:1 uniquement pour les espaces autorisant des lits :

- `spaceId UUID PK` ;
- `organizationId UUID` ;
- `spaceTypeCode varchar(64)` ;
- timestamps.

Contraintes :

- FK composite `(spaceId, organizationId, spaceTypeCode)` → FacilitySpace ;
- FK `spaceTypeCode` → `inpatient_space_type_catalog` ;
- seuls les types autorisés peuvent donc recevoir ce profil.

Dans ce lot, types autorisés : `HOSPITAL_ROOM`, `ICU_ROOM`.

### OrganizationalUnitSpaceAssignment

- `id UUID` ;
- `organizationId UUID` ;
- `organizationalUnitId UUID` ;
- `spaceId UUID` ;
- `validFrom Instant` ;
- `validTo Instant nullable` ;
- timestamps.

Règles :

- FK composite vers unité et espace du même tenant ;
- période valide ;
- pas de chevauchement pour le même couple `(organizationId, organizationalUnitId, spaceId)` ;
- différentes unités peuvent partager le même espace.

### BedEntity

Refactor de l'entité existante :

- `space` remplace `room` ;
- `bedNumber` conservé ;
- `status`, `capacityStatus`, `readinessStatus`, `version` conservés ;
- toutes les politiques de disponibilité/claim restent en place ;
- unicité `(organizationId, spaceId, bedNumber)`.

## 3. HospitalizationEntity

Le séjour doit cesser d'utiliser les chaînes comme identité.

Nouvelles références courantes :

- `current_service_unit_id UUID NOT NULL` ;
- `current_space_id UUID NOT NULL` ;
- `current_bed_id UUID NOT NULL`.

Snapshots :

- `service_name` renommé `service_name_snapshot` ;
- `room_number` renommé `space_name_snapshot` ;
- `bed_number` renommé `bed_number_snapshot`.

Les getters/API peuvent exposer des noms fonctionnels propres au nouveau contrat ; aucun alias de payload legacy n'est conservé.

Contraintes DB :

- current service unit du même tenant ;
- current space du même tenant ;
- `(currentBedId, currentSpaceId, organizationId)` référence le lit et garantit que le lit appartient au même espace ;
- le rattachement unité-espace actif est validé par le service applicatif au moment de l'admission/transfert.

## 4. Admission

Nouveau `CreateHospitalizationRequest` :

```java
UUID patientId;
UUID serviceUnitId;
UUID spaceId;
UUID bedId;
String admissionReason;
UUID visitId;
UUID emergencyId;
UUID responsiblePractitionerId;
```

Le constructeur backward-compatible existant est supprimé après migration de tous les tests/callers.

Validation :

1. résolution patient canonique ;
2. serviceUnit tenant + actif + type acceptable ;
3. espace tenant + actif + profil d'hébergement ;
4. assignment unité/espace actif à `Instant.now()` ;
5. bed tenant + `spaceId` exact ;
6. disponibilité atomique via `claimIfAvailable` ;
7. création du séjour ;
8. snapshots dérivés, jamais reçus du client.

Le `VisitEntity` créé automatiquement après urgence reçoit un snapshot de service dérivé du catalogue HOS-ORG, pas un texte du payload.

## 5. Transfert

Nouveau `TransferRequest` :

```java
UUID hospitalizationId;
UUID targetServiceUnitId;
UUID targetSpaceId;
UUID targetBedId;
```

Validation identique au couple unité/espace/lit de l'admission.

Après claim du lit cible :

1. libérer l'ancienne affectation ;
2. mettre le lit source en CLEANING selon le workflow existant ;
3. mettre à jour les références courantes + snapshots du séjour ;
4. créer la nouvelle affectation active ;
5. auditer IDs + snapshots lisibles.

## 6. Read models de capacité

Les anciennes projections `WardOccupancyResponse` / `RoomOccupancyResponse` sont remplacées par des read models orientés référentiels :

- capacité par `Space` ;
- agrégation par `OrganizationalUnit` grâce aux assignments actifs ;
- lecture par emplacement géographique.

Aucun compteur ne dépend d'un nom de service.

## 7. API configuration

Base maintenue : `/api/spatial/configuration`.

Contrats cibles :

- `GET /locations`
- `POST /locations`
- `PUT /locations/{id}`
- `POST /locations/{id}/activate|deactivate`
- `GET /spaces`
- `POST /spaces`
- `PUT /spaces/{id}`
- `POST /spaces/{id}/activate|deactivate`
- `POST /spaces/{id}/inpatient-profile`
- `DELETE /spaces/{id}/inpatient-profile` uniquement si aucun lit/historique ne l'interdit ; sinon 409
- `GET /space-types`
- `GET /unit-space-assignments`
- `POST /unit-space-assignments`
- `PUT /unit-space-assignments/{id}` pour clôture/correction autorisée
- `POST /beds`
- `PUT /beds/{id}` pour numéro/space dans les limites historiques définies

Routes legacy `/wards` et `/rooms` supprimées dans la même PR une fois consommateurs migrés.

## 8. Permission

`SPATIAL_CONFIGURATION_MANAGE` reste la permission de configuration physique.

Son libellé/documentation doit être aligné de :

> bâtiments, services, chambres et lits

vers :

> sites, bâtiments, étages, zones, espaces et lits

Elle ne crée ni ne modifie `OrganizationalUnit`.

## 9. Migration Flyway

Le lot utilise plusieurs migrations atomiques plutôt qu'un script monolithique :

### V88 — preflight legacy fail-fast (Java)

Première migration du lot. Elle ne modifie rien.

Elle bloque si des lignes existent dans les tables legacy incompatibles, au minimum :

- `wards` ;
- `rooms` ;
- `beds` ;
- `bed_assignments` ;
- `hospitalizations`.

Le message indique qu'un reset/remapping contrôlé est requis. Aucun nom n'est utilisé pour deviner une cible.

### V89 — nouveau modèle spatial (SQL portable)

- catalogues location/space ;
- location nodes ;
- spaces ;
- inpatient profiles ;
- unit-space assignments ;
- refactor beds `room_id → space_id` ;
- références structurées hospitalizations ;
- snapshots renommés ;
- suppression `rooms` puis `wards` ;
- indexes/FK/composites tenant.

### V90 — exclusion temporelle PostgreSQL (Java)

Ajoute une exclusion PostgreSQL `btree_gist` sur les périodes unité-espace ; sur H2, la règle reste couverte par le service/tests. Le projet utilise déjà ce pattern pour les affectations de lit historiques.

## 10. Sécurité / tenant

Défense en profondeur :

1. scope authentifié / plateforme explicite ;
2. `@TenantId` ;
3. repositories avec `organizationId` explicite ;
4. FK composites ;
5. IDs de tenant absents des payloads métier ;
6. cross-tenant retourné 404/403 sans fuite ;
7. aucune déduction par nom.

## 11. Angular

Nouvelle configuration spatiale remplace l'écran Ward/Room :

- arbre géographie ;
- liste/filtre espaces ;
- détail espace ;
- unités utilisatrices ;
- profil hébergement/lits si compatible ;
- formulaires par catalogues contrôlés.

Admission :

```text
Service / unité
   ↓
Espaces actifs rattachés
   ↓
Lits disponibles de l'espace
```

La logique de filtrage ergonomique peut être côté UI, mais le backend revalide toutes les règles.

## 12. Suppressions prévues après migration de tous les consommateurs

Backend :

- `WardEntity`, `WardRepository` ;
- `RoomEntity`, `RoomRepository` ;
- DTO/requests Ward/Room ;
- méthodes `SpatialConfigurationUseCase` Ward/Room ;
- méthodes `SpatialService` basées sur Ward/Room ;
- `HospitalServiceType` si plus aucun consommateur ;
- requêtes `findConfiguredBed` par noms.

Frontend :

- modèles/API/forms de configuration Ward/Room ;
- libellés i18n legacy correspondants ;
- tests fondés sur `serviceName/roomNumber/bedNumber` comme identité.

## 13. Taille de code

Le nouveau container Angular ne doit pas devenir un composant monolithique. Prévoir au minimum :

- page container ;
- location tree ;
- space list/detail ;
- space editor ;
- assignment editor ;
- inpatient/bed editor.

Cible : composants <300 lignes, maximum 500 lignes selon les règles du projet.
