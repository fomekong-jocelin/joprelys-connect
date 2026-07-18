# DATA-MODEL — Disponibilités médecins et prise de rendez-vous patient

> Statut : **livré** (STORY-2601 — migrations V70/V71, entités JPA et repositories).
> Ticket : `docs/ai/tickets/EPIC-0025-doctor-availability-appointments.md`
> Migrations : `backend/src/main/resources/db/migration/V70__create_appointment_tables.sql`, `V71__seed_appointment_permissions.sql`

## 1. Vue d'ensemble

Trois tables tenantées (colonne `organization_id`, isolation Hibernate `@TenantId` sur chaque entité) :

```text
organizations (id)
    │
    ├── doctor_availabilities ──► doctor_id ──► users (id)
    ├── doctor_availability_exceptions ──► doctor_id ──► users (id)
    └── appointments ──► doctor_id ──► users (id)
               │
               ├── patient_id ──► patients (id)
               └── visit_id ──► visits (id)   (ON DELETE SET NULL)
```

- `doctor_availabilities` : plages de disponibilité hebdomadaires récurrentes d'un médecin.
- `doctor_availability_exceptions` : indisponibilités ponctuelles (congés, absences).
- `appointments` : rendez-vous réservés, cycle de vie complet, lien vers la visite créée au check-in.

## 2. Table `doctor_availabilities`

| Colonne | Type | Null | Défaut | Commentaire |
|---|---|---|---|---|
| `id` | `UUID` | NON | — | PK, généré côté entité (`UUID.randomUUID()`) |
| `organization_id` | `UUID` | NON | — | FK `organizations(id)`, tenant |
| `doctor_id` | `UUID` | NON | — | FK `users(id)` `ON DELETE CASCADE` (`fk_doctor_availabilities_doctor`) |
| `weekday` | `INTEGER` | NON | — | 1 = lundi .. 7 = dimanche (ISO-8601), `ck_doctor_availabilities_weekday` |
| `start_time` | `TIME WITHOUT TIME ZONE` | NON | — | début de plage |
| `end_time` | `TIME WITHOUT TIME ZONE` | NON | — | fin de plage, `ck_doctor_availabilities_time_range` (`end_time > start_time`) |
| `valid_from` | `DATE` | NON | — | début d'application de la règle |
| `valid_to` | `DATE` | OUI | — | fin d'application, `ck_doctor_availabilities_validity` (`valid_to IS NULL OR valid_to >= valid_from`) |
| `active` | `BOOLEAN` | NON | `TRUE` | désactivation logique (jamais de suppression silencieuse, RM-07) |
| `created_at` / `updated_at` | `TIMESTAMP WITH TIME ZONE` | NON | — | posés par `@PrePersist`/`@PreUpdate` |

Index : `idx_doctor_availabilities_org_doctor (organization_id, doctor_id)`, `idx_doctor_availabilities_org_doctor_weekday (organization_id, doctor_id, weekday)`.

## 3. Table `doctor_availability_exceptions`

| Colonne | Type | Null | Commentaire |
|---|---|---|---|
| `id` | `UUID` | NON | PK |
| `organization_id` | `UUID` | NON | FK `organizations(id)`, tenant |
| `doctor_id` | `UUID` | NON | FK `users(id)` `ON DELETE CASCADE` (`fk_doctor_availability_exceptions_doctor`) |
| `start_at` | `TIMESTAMP WITH TIME ZONE` | NON | début d'indisponibilité |
| `end_at` | `TIMESTAMP WITH TIME ZONE` | NON | fin d'indisponibilité, `ck_doctor_availability_exceptions_range` (`end_at > start_at`) |
| `reason` | `VARCHAR(255)` | OUI | motif libre |
| `created_at` / `updated_at` | `TIMESTAMP WITH TIME ZONE` | NON | |

Index : `idx_doctor_availability_exceptions_org_doctor (organization_id, doctor_id)`, `idx_doctor_availability_exceptions_org_doctor_period (organization_id, doctor_id, start_at)`.

## 4. Table `appointments`

| Colonne | Type | Null | Défaut | Commentaire |
|---|---|---|---|---|
| `id` | `UUID` | NON | — | PK |
| `organization_id` | `UUID` | NON | — | FK `organizations(id)`, tenant |
| `doctor_id` | `UUID` | NON | — | FK `users(id)` (`fk_appointments_doctor`, pas de cascade) |
| `patient_id` | `UUID` | NON | — | FK `patients(id)` (`fk_appointments_patient`, pas de cascade) |
| `start_at` | `TIMESTAMP WITH TIME ZONE` | NON | — | début du créneau |
| `end_at` | `TIMESTAMP WITH TIME ZONE` | NON | — | fin du créneau, `ck_appointments_time_range` (`end_at > start_at`) |
| `status` | `VARCHAR(32)` | NON | `'CONFIRMED'` | enum `AppointmentStatus` persisté en String (pattern `DocumentStatus`) — **pas de CHECK** (convention V68) |
| `reason` | `TEXT` | OUI | — | motif du rendez-vous |
| `cancelled_at` | `TIMESTAMP WITH TIME ZONE` | OUI | — | horodatage d'annulation |
| `cancellation_reason` | `VARCHAR(255)` | OUI | — | motif d'annulation |
| `visit_id` | `UUID` | OUI | — | FK `visits(id)` `ON DELETE SET NULL` (`fk_appointments_visit`) — lien posé au check-in |
| `reminder_sent_at` | `TIMESTAMP WITH TIME ZONE` | OUI | — | marqueur du rappel e-mail (STORY-2605) |
| `active_start_at` | `TIMESTAMP WITH TIME ZONE` | OUI | — | **colonne technique anti double réservation** (voir §5) |
| `created_at` / `updated_at` | `TIMESTAMP WITH TIME ZONE` | NON | — | |

Index :
- `uq_appointments_doctor_active_slot` **UNIQUE** `(doctor_id, active_start_at)` — garantit un seul RDV actif par médecin et par créneau.
- `idx_appointments_org_doctor_start (organization_id, doctor_id, start_at)` — agenda médecin.
- `idx_appointments_org_patient (organization_id, patient_id)` — historique patient.
- `idx_appointments_org_start (organization_id, start_at)` — cahier de rendez-vous de l'accueil.

## 5. Règles de maintien de `active_start_at`

`active_start_at` est une colonne **technique**, jamais exposée ni passée au constructeur métier ; elle est recalculée par l'entité dans `@PrePersist` et `@PreUpdate` :

```text
active_start_at = start_at   si status ∈ {CONFIRMED, COMPLETED, NO_SHOW}
active_start_at = NULL       si status ∈ {CANCELLED_BY_PATIENT, CANCELLED_BY_CLINIC}
```

Conséquences :
- L'index unique `(doctor_id, active_start_at)` interdit deux RDV **actifs** au même créneau pour un même médecin (RM-02).
- PostgreSQL et H2 autorisent les `NULL` multiples dans un index unique : plusieurs RDV annulés peuvent coexister sur le même créneau, et un créneau annulé redevient réservable.
- `COMPLETED` et `NO_SHOW` conservent le créneau : un RDV honoré ou une absence marquée ne libèrent pas le passé (traçabilité), seule l'annulation libère.

## 6. Cycle de vie des statuts (`AppointmentStatus`)

```text
                 réservation
                      ▼
                CONFIRMED ────── check-in + clôture visite ──► COMPLETED
                    │
                    ├── annulation patient (≤ délai) ──► CANCELLED_BY_PATIENT
                    ├── annulation clinique ───────────► CANCELLED_BY_CLINIC
                    └── absence constatée ─────────────► NO_SHOW
```

Statuts terminaux : `CANCELLED_BY_PATIENT`, `CANCELLED_BY_CLINIC`, `COMPLETED`, `NO_SHOW`. Le statut par défaut `CONFIRMED` est posé dans le constructeur Java (pattern `status = "EN_COURS"` de `VisitEntity`) et en défaut de colonne SQL.

## 7. Écarts justifiés au ticket initial

### D1 — Structure du module sans `domain/`
Le module est structuré en `api/ application/ infrastructure/persistence/` (pattern constaté sur les modules récents, ex. `visit`). Le dossier `domain/` annoncé dans `TECHNICAL-DESIGN.md` §2 n'est pas créé : il n'a pas d'usage avéré pour ces fondations (les invariants tiennent dans les entités et futurs services) ; la doc est corrigée par la présente.

### D2 — `active_start_at` au lieu de l'index partiel PostgreSQL
Le `TECHNICAL-DESIGN.md` §5 proposait `UNIQUE (doctor_id, start_at) WHERE status NOT IN ('CANCELLED_BY_PATIENT','CANCELLED_BY_CLINIC')`. Vérification faite (lecture des migrations V1→V69), **aucune migration existante n'utilise d'index partiel** (`CREATE ... INDEX ... WHERE`), et la suite de tests d'intégration tourne sur H2 en mode PostgreSQL qui ne le supporterait pas. La colonne nullable maintenue `active_start_at` + index unique simple offre la même garantie de façon portable H2/PostgreSQL.

### D3 — V71 = `permissions` seulement
V71 n'insère que les 3 permissions (`INSERT ... SELECT ... WHERE NOT EXISTS`, idempotent, pattern V64). Les liens rôle → permission ne sont **pas** seedés en SQL : ils sont possédés par `RbacCatalog.systemRoles()` et réappliqués à chaque démarrage par `RbacStore.seedCatalog()`, qui supprime puis réinsère les `role_permissions` des rôles système — un seed SQL serait de toute façon écrasé.

### D4 — `AppointmentStatus` en enum persisté String
`AppointmentStatus` est un petit enum Java persisté via `@Enumerated(EnumType.STRING)`, copie exacte du pattern `visit/infrastructure/persistence/DocumentStatus.java`. Aucun CHECK de statut en base (V68 les a retirés).

### D5 — Statut par défaut en constructeur
`status = AppointmentStatus.CONFIRMED` dans le constructeur métier (pattern `VisitEntity`), doublé du `DEFAULT 'CONFIRMED'` SQL pour les insertions hors JPA.

## 8. Conventions appliquées

- PK `id UUID PRIMARY KEY` (génération applicative, pas de `@GeneratedValue`).
- `organization_id UUID NOT NULL REFERENCES organizations(id)` + `@TenantId` sur chaque entité.
- FK nommées `fk_<table>_<role>` ; index nommés `idx_<table>_<cols>`, dont systématiquement un préfixé par `organization_id`.
- Timestamps `TIMESTAMP WITH TIME ZONE NOT NULL` sans défaut SQL (posés par l'entité).
- Aucune contrainte CHECK sur la colonne de statut (convention V68) ; CHECK conservés pour les invariants temporels et bornes.
- Migrations additives uniquement, sans `IF NOT EXISTS` sur les tables ; rollback = tables inutilisées.
