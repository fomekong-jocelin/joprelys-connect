# TECHNICAL-DESIGN — STORY-2603 Prise de rendez-vous patient

## 1. Stack et modules impactés

- Spring Boot / Maven : module `com.joprelys.backend.appointment`.
- JPA/Hibernate multi-tenant : `AppointmentEntity`, repositories existants.
- Sécurité : `@PreAuthorize("hasRole('PATIENT')")`, `PatientAccessGuardService`.
- Angular standalone / Tailwind CSS v4 : portail patient.
- i18n : dictionnaire fonctionnel `features/appointments` en français et anglais.
- CI : Maven `clean verify`, tests Angular et build production.

## 2. Architecture backend

```text
PatientAppointmentController
        ↓
PatientAppointmentService (interface)
        ↓
DefaultPatientAppointmentService (@Transactional)
        ├── PatientAccessGuardService
        ├── AvailabilityService
        ├── UserAccountRepository
        └── AppointmentRepository
```

Le contrôleur ne contient aucune règle métier ni accès repository. Les DTO sont des records immuables et les entités JPA ne sont jamais exposées.

## 3. Contrats API

Base : `/api/patient/appointments`

- `GET /doctors?specialty=&department=` : annuaire same-tenant.
- `GET /slots?doctorId=&from=&to=` : créneaux réservables.
- `POST /` : réservation.
- `GET /?from=&to=` : rendez-vous du patient authentifié.
- `POST /{id}/cancel` : annulation patient.

Le contrat détaillé reste dans `docs/features/doctor-availability-appointments/API-CONTRACT.md` §4.

## 4. Validation médecin

La table `users` n’est pas filtrée par `@TenantId`. Toute résolution médecin vérifie explicitement :

- identifiant présent ;
- `organizationId` identique au patient ;
- compte activé ;
- rôle CSV contenant `MEDECIN`.

Une absence ou un écart tenant retourne `404 DOCTOR_NOT_FOUND`.

## 5. Calcul des créneaux

1. Valider la période et l’horizon.
2. Appeler `AvailabilityService.generateSlots(doctorId, from, to)`.
3. Filtrer les créneaux avec `slot.startAt >= max(now, from)` afin de ne jamais retourner un créneau déjà commencé.
4. Limiter `to` à l’horizon configurable.
5. Retourner les instants UTC ; Angular applique le format local.

## 6. Réservation transactionnelle

La réservation est exécutée dans une seule transaction :

1. résoudre le patient authentifié ;
2. verrouiller pessimiste le compte médecin (`PESSIMISTIC_WRITE`) pour sérialiser les réservations de ce médecin ;
3. revalider médecin, futur et horizon ;
4. recalculer les créneaux ;
5. vérifier que l’instant demandé correspond exactement à un créneau généré ;
6. vérifier RM-04 dans les bornes de la journée clinique ;
7. créer `AppointmentEntity` et `saveAndFlush()` ;
8. mapper toute violation de `uq_appointments_doctor_active_slot` vers `409 SLOT_UNAVAILABLE`.

Le verrou médecin évite la course `exists → save` pour RM-04. L’index unique reste le dernier arbitre pour le double-booking exact.

## 7. Annulation

- Charger le rendez-vous via le tenant courant.
- Vérifier que son patient est celui résolu par `PatientAccessGuardService`; sinon retourner 404.
- Accepter uniquement `CONFIRMED`.
- Calculer la date limite : `startAt - patientCancelDeadlineHours`.
- Autoriser lorsque `now <= deadline`, refuser lorsque `now > deadline`.
- Appeler `appointment.cancel(CANCELLED_BY_PATIENT, reason)` puis `saveAndFlush()`.
- Aucun bulk update JPQL/SQL.

## 8. D7 — Création d’indisponibilité

Avant la sauvegarde d’une nouvelle `DoctorAvailabilityExceptionEntity`, rechercher les rendez-vous actifs futurs du médecin et refuser tout chevauchement strict avec `409 AVAILABILITY_CONFLICT`.

## 9. Repositories

Ajouts prévus :

- verrou pessimiste médecin par identifiant ;
- rendez-vous d’un patient avec filtres temporels ;
- rendez-vous actifs patient/médecin dans une journée clinique ;
- rendez-vous par identifiant et patient pour anti-IDOR ;
- médecins actifs d’un établissement, triés par nom.

Toutes les requêtes restent paramétrées et tenant-safe.

## 10. Architecture Angular

```text
patient/portal/appointments/
  pages/patient-appointments-page.component.ts
  components/doctor-directory.component.ts
  components/patient-appointment-list.component.ts
  services/patient-appointments-api.service.ts
  models/patient-appointments.models.ts
shared/ui/appointment-slot-picker/
```

- La page orchestre l’état par signals.
- Le service encapsule `HttpClient` avec des URLs relatives.
- Les composants de présentation utilisent inputs/outputs.
- Le backend reste source de vérité ; le frontend ne calcule ni horizon ni éligibilité d’annulation comme décision finale.
- Après `SLOT_UNAVAILABLE`, les créneaux sont rechargés.

## 11. Sécurité

- AuthN/AuthZ sur chaque endpoint.
- Anti-IDOR : aucun `patientId` en entrée.
- 404 pour un rendez-vous appartenant à un autre patient ou tenant.
- Aucune PII dans les logs ou les erreurs techniques.
- Aucune donnée sensible en `localStorage`.
- Validation Bean Validation aux frontières.

## 12. Configuration

Aucune nouvelle clé. Réutilisation de :

- `joprelys.appointments.default-slot-duration-minutes` ;
- `joprelys.appointments.booking-horizon-days` ;
- `joprelys.appointments.patient-cancel-deadline-hours` ;
- `joprelys.appointments.timezone`.

## 13. Observabilité

- Réutiliser `X-Trace-Id` et le format `ApiErrorResponse`.
- Ne jamais journaliser le motif médical du rendez-vous.
- Les conflits métier sont exposés par code stable, sans stack trace.

## 14. Impact données

Aucune migration prévue : les garanties reposent sur l’index unique existant et le verrou pessimiste du médecin. Toute nécessité de nouvelle contrainte découverte en validation imposera une migration additive et une mise à jour de `DATA-MODEL.md`.

## 15. SemVer

**MINOR** — ajout rétrocompatible d’API et d’interface patient.
