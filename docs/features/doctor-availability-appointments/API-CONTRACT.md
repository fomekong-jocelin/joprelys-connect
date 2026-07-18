# API-CONTRACT — Disponibilités médecins et prise de rendez-vous patient

> Statut : **contrat cible** figé par STORY-2601. Implémentation prévue : STORY-2602 (`/api/availabilities/**`), STORY-2603 (`/api/patient/appointments/**`), STORY-2604 (`/api/appointments/**`).
> Ticket : `docs/ai/tickets/EPIC-0025-doctor-availability-appointments.md`

## 1. Principes communs

- Base `/api`, authentification JWT Bearer, autorisation *deny-by-default* (`SecurityConfig`).
- Autorisation par rôle via `@PreAuthorize`, pattern existant `hasAnyRole(...)` (ex. `VisitController`).
- Format d'erreur normalisé `ApiErrorResponse` (`common/api`) :

```json
{
  "error": {
    "code": "SLOT_UNAVAILABLE",
    "message": "Ce créneau n'est plus disponible.",
    "trace_id": "trc_..."
  }
}
```

- Codes HTTP : `200` succès (y compris créations, pattern existant), `400` règle métier/validation, `401` non authentifié, `403` rôle insuffisant, `404` ressource introuvable (ou hors tenant), `409` conflit d'état.
- Multi-tenant : toutes les ressources sont filtrées par `organization_id` (`@TenantId`) ; une ressource d'un autre tenant répond `404`.
- Traçabilité : `X-Trace-Id` propagé (`TraceIdFilter`) ; aucune PII dans les logs.
- Dates/heures : ISO-8601 (`Instant` UTC pour `startAt`/`endAt`, `HH:mm` pour les plages horaires, `yyyy-MM-dd` pour les dates).

> **Point de vigilance (revue)** : le mapping permissions → `GrantedAuthority` du filtre JWT n'a pas été inspecté dans le cadre de STORY-2601. Le choix `@PreAuthorize("hasAnyRole(...)")` suit strictement l'existant (`VisitController`, `DocumentController`…). Si le filtre mappe aussi les permissions en authorities, une évolution vers `hasAuthority('APPOINTMENT_READ')` reste possible sans rupture de contrat.

## 2. `/api/availabilities/**` — STORY-2602

Rôles : `MEDECIN` (ses propres règles), `ADMIN_CLINIQUE` (tous les médecins de la clinique). Permission catalogue : `AVAILABILITY_MANAGE`.

| Méthode | Path | `@PreAuthorize` | Objet |
|---|---|---|---|
| GET | `/api/availabilities?doctorId=` | `hasAnyRole('MEDECIN','ADMIN_CLINIQUE')` | liste des règles récurrentes (du médecin connecté si `doctorId` omis) |
| POST | `/api/availabilities` | `hasAnyRole('MEDECIN','ADMIN_CLINIQUE')` | création d'une règle |
| PUT | `/api/availabilities/{id}` | `hasAnyRole('MEDECIN','ADMIN_CLINIQUE')` | modification d'une règle |
| DELETE | `/api/availabilities/{id}` | `hasAnyRole('MEDECIN','ADMIN_CLINIQUE')` | désactivation logique (`active=false`, jamais de suppression silencieuse — RM-07) |
| GET | `/api/availabilities/exceptions?doctorId=&from=&to=` | `hasAnyRole('MEDECIN','ADMIN_CLINIQUE')` | liste des indisponibilités |
| POST | `/api/availabilities/exceptions` | `hasAnyRole('MEDECIN','ADMIN_CLINIQUE')` | création d'une indisponibilité |
| DELETE | `/api/availabilities/exceptions/{id}` | `hasAnyRole('MEDECIN','ADMIN_CLINIQUE')` | suppression d'une indisponibilité |

`UpsertAvailabilityRuleRequest` : `{ "doctorId": uuid?, "weekday": 1..7, "startTime": "HH:mm", "endTime": "HH:mm", "validFrom": "yyyy-MM-dd", "validTo": "yyyy-MM-dd"? }` — `doctorId` omis = médecin connecté ; un `MEDECIN` ne peut viser que lui-même (`403` sinon).

`AvailabilityRuleResponse` : `{ "id", "doctorId", "weekday", "startTime", "endTime", "validFrom", "validTo", "active", "createdAt", "updatedAt" }`.

`CreateAvailabilityExceptionRequest` : `{ "doctorId": uuid?, "startAt": instant, "endAt": instant, "reason": string? }`.

`AvailabilityExceptionResponse` : `{ "id", "doctorId", "startAt", "endAt", "reason", "createdAt", "updatedAt" }`.

Erreurs dédiées : `VALIDATION_ERROR` (400 — plage invalide, `endTime <= startTime`, `validTo < validFrom`), `AVAILABILITY_OVERLAP` (409 — chevauchement de règles actives), `AVAILABILITY_CONFLICT` (409 — des RDV existants sur la plage : action explicite requise, RM-07), `AVAILABILITY_NOT_FOUND` (404).

## 3. `/api/appointments/**` — STORY-2604

Rôles : `AGENT_ACCUEIL`, `ADMIN_CLINIQUE` ; la lecture est aussi ouverte au `MEDECIN` (restreinte à son propre agenda — il possède `APPOINTMENT_READ` mais pas `APPOINTMENT_WRITE`). Permissions catalogue : `APPOINTMENT_READ` (lecture), `APPOINTMENT_WRITE` (mutations).

| Méthode | Path | `@PreAuthorize` | Objet |
|---|---|---|---|
| GET | `/api/appointments?date=&doctorId=&status=` | `hasAnyRole('AGENT_ACCUEIL','MEDECIN','ADMIN_CLINIQUE')` | cahier du jour/semaine, filtres date (`yyyy-MM-dd`), médecin, statut |
| GET | `/api/appointments/{id}` | `hasAnyRole('AGENT_ACCUEIL','MEDECIN','ADMIN_CLINIQUE')` | détail d'un RDV |
| POST | `/api/appointments` | `hasAnyRole('AGENT_ACCUEIL','ADMIN_CLINIQUE')` | réservation assistée pour un patient |
| POST | `/api/appointments/{id}/check-in` | `hasAnyRole('AGENT_ACCUEIL','ADMIN_CLINIQUE')` | crée la `VisitEntity` liée (`visit_id`) dans la file d'attente existante |
| POST | `/api/appointments/{id}/cancel` | `hasAnyRole('AGENT_ACCUEIL','ADMIN_CLINIQUE')` | annulation clinique → `CANCELLED_BY_CLINIC` |
| POST | `/api/appointments/{id}/no-show` | `hasAnyRole('AGENT_ACCUEIL','ADMIN_CLINIQUE')` | marque l'absence → `NO_SHOW` |

`ReceptionBookAppointmentRequest` : `{ "patientId": uuid, "doctorId": uuid, "startAt": instant, "reason": string? }` — `endAt` calculé côté serveur (`startAt + default-slot-duration-minutes`).

`CancelAppointmentRequest` : `{ "cancellationReason": string? }`.

`AppointmentResponse` : `{ "id", "doctorId", "patientId", "startAt", "endAt", "status", "reason", "cancelledAt", "cancellationReason", "visitId", "reminderSentAt", "createdAt", "updatedAt" }`.

Erreurs dédiées : `SLOT_UNAVAILABLE` (409 — créneau déjà réservé ou hors disponibilités), `BOOKING_HORIZON_EXCEEDED` (400 — au-delà de `booking-horizon-days`), `PAST_SLOT` (400), `APPOINTMENT_NOT_FOUND` (404), `PATIENT_NOT_FOUND` (404), `INVALID_STATUS_TRANSITION` (409 — ex. check-in d'un RDV annulé), `VISIT_ALREADY_LINKED` (409).

## 4. `/api/patient/appointments/**` — STORY-2603

Rôle : `PATIENT` (portail patient existant) — `@PreAuthorize("hasRole('PATIENT')")`, pattern `PatientPortalController`. Le patient est résolu côté serveur via `PatientAccessGuardService` (aucun `patientId` en entrée, anti-IDOR).

| Méthode | Path | `@PreAuthorize` | Objet |
|---|---|---|---|
| GET | `/api/patient/appointments/doctors?specialty=&department=` | `hasRole('PATIENT')` | annuaire des médecins de la clinique (nom, spécialité, service) |
| GET | `/api/patient/appointments/slots?doctorId=&from=&to=` | `hasRole('PATIENT')` | créneaux libres (règles − exceptions − RDV actifs) |
| POST | `/api/patient/appointments` | `hasRole('PATIENT')` | réservation transactionnelle |
| GET | `/api/patient/appointments?from=&to=` | `hasRole('PATIENT')` | « mes rendez-vous » (tri antéchronologique) |
| POST | `/api/patient/appointments/{id}/cancel` | `hasRole('PATIENT')` | annulation → `CANCELLED_BY_PATIENT` (RM-05) |

`DoctorDirectoryEntry` : `{ "doctorId", "displayName", "specialty", "department" }`.

`SlotResponse` : `{ "doctorId", "startAt", "endAt" }`.

`PatientBookAppointmentRequest` : `{ "doctorId": uuid, "startAt": instant, "reason": string? }`.

Erreurs dédiées : `SLOT_UNAVAILABLE` (409), `BOOKING_HORIZON_EXCEEDED` (400), `PAST_SLOT` (400), `DUPLICATE_ACTIVE_APPOINTMENT` (409 — un seul RDV actif par patient/médecin/jour, RM-04), `CANCEL_DEADLINE_PASSED` (400 — annulation au-delà de `patient-cancel-deadline-hours`), `APPOINTMENT_NOT_FOUND` (404 — y compris RDV d'un autre patient), `DOCTOR_NOT_FOUND` (404).

## 5. Référentiel de codes d'erreur

| Code | HTTP | Déclencheur |
|---|---|---|
| `SLOT_UNAVAILABLE` | 409 | créneau hors plage, masqué par une exception ou déjà réservé (conflit d'unicité `uq_appointments_doctor_active_slot`) |
| `BOOKING_HORIZON_EXCEEDED` | 400 | réservation au-delà de `joprelys.appointments.booking-horizon-days` (défaut 30 j) |
| `CANCEL_DEADLINE_PASSED` | 400 | annulation patient après `joprelys.appointments.patient-cancel-deadline-hours` (défaut 24 h) |
| `PAST_SLOT` | 400 | créneau dans le passé (RM-03) |
| `DUPLICATE_ACTIVE_APPOINTMENT` | 409 | RM-04 (patient/médecin/jour) |
| `INVALID_STATUS_TRANSITION` | 409 | transition interdite (ex. annulation d'un RDV `COMPLETED`) |
| `VISIT_ALREADY_LINKED` | 409 | double check-in |
| `AVAILABILITY_OVERLAP` | 409 | règles actives qui se chevauchent |
| `AVAILABILITY_CONFLICT` | 409 | modification de plage avec RDV existants (RM-07) |
| `APPOINTMENT_NOT_FOUND` / `AVAILABILITY_NOT_FOUND` / `DOCTOR_NOT_FOUND` / `PATIENT_NOT_FOUND` | 404 | ressource absente ou hors tenant |
| `VALIDATION_ERROR` | 400 | payload invalide |
| `ACCESS_DENIED` | 403 | rôle insuffisant |

## 6. Rappel des règles métier adossées au contrat

- RM-01 durée de créneau configurable (`joprelys.appointments.default-slot-duration-minutes`, défaut 30 min).
- RM-02 double réservation impossible (index unique §5 DATA-MODEL + transaction service).
- RM-03 réservation dans le futur et l'horizon configuré.
- RM-04 un RDV actif par patient/médecin/jour.
- RM-05 délai d'annulation patient configurable.
- RM-07 aucune suppression silencieuse de RDV lors d'un changement de disponibilité.
- RM-08 check-in → création visite liée ; clôture visite → `COMPLETED`.
- RM-09 isolation tenant stricte ; RM-10 audit sans PII.
