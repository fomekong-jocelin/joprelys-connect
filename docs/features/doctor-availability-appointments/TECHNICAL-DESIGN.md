# TECHNICAL-DESIGN — Disponibilités médecins et prise de rendez-vous patient

> Statut : **initiale** (Documentation First — démarrage EPIC-0025). Évolue avec le code.
> Ticket : `docs/ai/tickets/EPIC-0025-doctor-availability-appointments.md`

## 1. Stack concernée

- Backend : Spring Boot 4.1.0 / Java 21, Maven (`mvnw`), Spring Security (JWT maison), Spring Data JPA, Flyway, PostgreSQL (H2 mode PostgreSQL en test, Testcontainers en CI).
- Frontend : Angular 22 standalone + signals, Tailwind CSS v4 CSS-first, Vitest, proxy `proxy.conf.json` → chemins relatifs `/api`.
- DB : migrations Flyway **V70+** (dernière existante : V69).

## 2. Modules / fichiers probablement impactés

### Backend (nouveau module, structure maison `api / application / domain / infrastructure`)

```text
backend/src/main/java/com/joprelys/backend/appointment/
  api/            AppointmentController, PatientAppointmentController, AvailabilityController, DTO records
  application/    services (contrats + implémentations), génération de créneaux, politique d'annulation
  domain/         invariants et règles (sans dépendance framework)
  infrastructure/ entités JPA, repositories
backend/src/main/resources/db/migration/V70__create_appointment_tables.sql
backend/src/main/resources/db/migration/V71__seed_appointment_rbac.sql
backend/.../auth/rbac/RbacCatalog.java   (permissions à ajouter)
backend/src/main/resources/application.yml  (bloc joprelys.appointments.*)
```

### Frontend

```text
web/src/app/appointment/                    (feature clinique : disponibilités, cahier RDV)
web/src/app/patient/portal/pages/appointments/   (pages portail patient)
web/src/app/shared/ui/                      (calendar, time-slot-picker — composants maison)
web/src/app/app.routes.ts                   (routes + roleGuard expectedRoles)
web/src/assets/i18n/features/appointments.{fr,en}.json  (nouveau dictionnaire)
web/src/app/core/i18n/i18n.service.ts       (enregistrement du dictionnaire — liste hardcodée :43-52)
DESIGN.md                                   (composants calendrier à documenter)
```

## 3. Architecture cible

- Flux strict `Controller → Application/Service → Domain ← Infrastructure` (AGENTS.md, SOLID) : controllers sans logique métier, transactions au niveau service, entités jamais exposées (DTO records avec `fromEntity`).
- **Génération de créneaux côté backend** (backend maître de la vérité) : `créneaux = plages récurrentes ∩ période demandée − exceptions − créneaux déjà réservés (statuts actifs)`.
- Résolution patient côté serveur via `PatientAccessGuardService` (tokens patient sans `patient_id`, lien par `globalPatientNumber`).
- Filtrage des médecins : réutiliser le pattern de filtrage du rôle CSV multi-rôles de `StaffService` (`UserAccountEntity.role` est une String CSV).
- Multi-tenant : colonne `organization_id` + `@TenantId` sur toutes les entités du module, index par tenant (pattern V4).

## 4. Contrats API attendus (esquisse — à finaliser dans `API-CONTRACT.md`, STORY-2601)

### Côté clinique (staff)

| Méthode | Route | Rôles / permissions | Objet |
|---|---|---|---|
| `GET/POST/PUT/DELETE` | `/api/availabilities/**` | `MEDECIN` (soi-même), `ADMIN_CLINIQUE` (`AVAILABILITY_MANAGE`) | Règles récurrentes + exceptions |
| `GET` | `/api/appointments?date=&doctorId=&status=` | `AGENT_ACCUEIL`, `MEDECIN`, `ADMIN_CLINIQUE` (`APPOINTMENT_READ`) | Cahier de RDV |
| `POST` | `/api/appointments` | `AGENT_ACCUEIL` (`APPOINTMENT_WRITE`) | Réservation pour un patient |
| `POST` | `/api/appointments/{id}/check-in` | `AGENT_ACCUEIL` | Crée la visite liée |
| `POST` | `/api/appointments/{id}/cancel` / `no-show` | `AGENT_ACCUEIL`, `ADMIN_CLINIQUE` | Cycle de vie |

### Côté portail patient (`@PreAuthorize("hasRole('PATIENT')")`, pattern `PatientPortalController`)

| Méthode | Route | Objet |
|---|---|---|
| `GET` | `/api/patient/appointments/doctors?specialty=&department=` | Annuaire médecins de la clinique |
| `GET` | `/api/patient/appointments/availability?doctorId=&from=&to=` | Créneaux libres |
| `POST` | `/api/patient/appointments` | Réservation (transactionnelle) |
| `GET` | `/api/patient/appointments` | Mes rendez-vous |
| `POST` | `/api/patient/appointments/{id}/cancel` | Annulation (règle RM-05) |

Erreurs : format `ApiErrorResponse{code,message,traceId}` existant ; codes dédiés (`SLOT_UNAVAILABLE`, `CANCEL_DEADLINE_PASSED`, `BOOKING_HORIZON_EXCEEDED`…).

## 5. Modèle de données / migrations (esquisse — à finaliser dans `DATA-MODEL.md`)

- `doctor_availabilities` : `id uuid pk`, `organization_id uuid not null`, `doctor_id uuid not null references users(id)`, `weekday smallint (0-6)`, `start_time`, `end_time`, `slot_duration_minutes int default 30`, `valid_from date`, `valid_to date null`, `active boolean`, timestamps. Index `(organization_id, doctor_id)`.
- `doctor_availability_exceptions` : `id`, `organization_id`, `doctor_id`, `start_at timestamptz`, `end_at timestamptz`, `reason varchar null`, timestamps.
- `appointments` : `id`, `organization_id`, `patient_id uuid not null references patients(id)`, `doctor_id uuid not null references users(id)`, `start_at timestamptz not null`, `end_at timestamptz not null`, `status varchar not null default 'CONFIRMED'`, `reason varchar null`, `booked_by varchar not null` (`PATIENT`/`STAFF`), `visit_id uuid null references visits(id)`, `cancelled_at`, `cancelled_by`, timestamps. **Index unique partiel `(doctor_id, start_at) WHERE status NOT IN ('CANCELLED_BY_PATIENT','CANCELLED_BY_CLINIC')`** (anti double réservation). Index `(organization_id, patient_id)`, `(organization_id, doctor_id, start_at)`.
- V71 : seed permissions `APPOINTMENT_READ`, `APPOINTMENT_WRITE`, `AVAILABILITY_MANAGE` + affectations rôles (pattern V57).

## 6. Configuration nécessaire

```yaml
joprelys:
  appointments:
    default-slot-duration-minutes: 30
    booking-horizon-days: 30
    patient-cancel-deadline-hours: 24
    reminder-hours-before: 24
```

YAML uniquement (jamais `application.properties`) ; valeurs surchargeables par variables d'environnement ; aucune URL backend en dur côté Angular.

## 7. Sécurité et permissions

- `@PreAuthorize` par méthode ; deny-by-default déjà actif (`SecurityConfig`).
- Isolation tenant : `@TenantId` + `TenantContext` (claim `org`) ; tests d'isolation obligatoires.
- Concurrence : contrainte unique partielle + transaction au niveau service ; test de concurrence double réservation (pattern `AuthSessionRotationConcurrencyTest`).
- Pas de PII dans les logs ; actions auditées via le module `audit` existant.
- RBAC : nouvelles permissions cataloguées (`RbacCatalog`) et seedées en migration.

## 8. UI / design system

- Respect de `DESIGN.md` : tokens centralisés `styles.css`, arrondis ≤ 8 px (`rounded-sm/lg`, jamais `rounded-full/2xl/3xl` sur cards/inputs/boutons), ombres légères, light/dark obligatoires.
- Composants calendrier / sélecteur de créneaux **maison** dans `shared/ui` (Angular Material et libs tierces interdites) ; accessibilité clavier et contrastes WCAG dans les deux thèmes.
- i18n : aucun texte en dur ; nouveau dictionnaire `features/appointments` FR/EN + enregistrement dans `I18nService.loadLocale()`.
- Composants < 500 lignes (alerte 300) ; composants orientés présentation, logique dans les services/facades.

## 9. Logs / observabilité attendus

- Logs métier `INFO` (réservation, annulation, check-in) avec `traceId` (MDC existant), identifiants techniques uniquement (pas de nom patient/médecin).
- Erreurs normalisées via `GlobalExceptionHandler`.

## 10. Stratégie de tests

- Backend : unitaires (génération créneaux : chevauchements, exceptions, bornes ; politique d'annulation), intégration MockMvc (AuthN/AuthZ allowed/denied, isolation tenant), Testcontainers (migrations V70/V71 sur PostgreSQL 16), concurrence double réservation, intégration conversion RDV → visite.
- Frontend : Vitest — composants shared/ui, pages portail, guard, service API ; états chargement/erreur/vide.
- Commandes : `cd backend && ./mvnw test` ; `cd web && npm run test && npm run build` (et `npm run lint` si configuré).

## 11. Impact SemVer prévu

**MINOR** — fonctionnalité nouvelle rétrocompatible : aucun endpoint/contrat/table existant modifié, migrations additives uniquement. Rollback : désactivation des routes + tables inutilisées (aucune destruction).

## 12. Points d'attention / dette à ne pas reproduire

- Liste des dictionnaires i18n hardcodée dans `i18n.service.ts` → penser à y enregistrer le nouveau dictionnaire (sinon clés brutes affichées, cf. BUG-20260716-I18N-SHELL-MISSING-KEYS).
- Ne pas créer d'écran monolithique (dette identifiée en suivi global).
- `Visit.mainPractitionerId` est un UUID brut : le lien RDV → médecin passe par une vraie FK `appointments.doctor_id → users(id)`.

## 13. Cloisonnement de contexte d'authentification

Correctif P0 `BUG-20260718-PATIENT-PROFESSIONAL-RBAC-CONTEXT-LEAK` :

- le cache `RbacApiService` est associé au jeton d'accès ayant déclenché `/api/rbac/me` ;
- une réponse asynchrone appartenant à un ancien jeton ne peut pas devenir le contexte effectif courant ;
- `AppShellNavComponent` traite `PATIENT` comme un mode exclusif et ne mélange jamais les permissions professionnelles ;
- `roleGuard` refuse une route ne déclarant pas `PATIENT` avant toute résolution RBAC ;
- `JwtAuthenticationFilter` ne crée que `ROLE_PATIENT` lorsqu'un token est classé patient, même en présence d'un claim de rôles mixte ;
- les contrôleurs restent la source de vérité ; `/api/availabilities/**` exige `hasAuthority('AVAILABILITY_MANAGE')` et des tests de refus patient explicites.

## 14. Ergonomie Mobile-First & Résilience des Endpoints (Correctif P0 du 2026-07-28)

Correctif `BUG-20260728-DOCTOR-AVAILABILITY-MOBILE-GRID-AND-WEEKDAY-BUG` :

- **Routage Backend résilient** : `@RequestMapping({"/api/availabilities", "/api/availabilities/"})` dans `AvailabilityController.java` pour supporter les réécritures proxy/Nginx avec ou sans trailing slash sans générer de faux 404.
- **Grille Mobile-First `WeeklyAvailabilityGridComponent`** :
  - **Mobile (< 768px)** : Affichage d'un bandeau horizontal d'onglets pour naviguer entre les jours (Lun → Dim). Timeline verticale du jour avec boutons tactiles ergonomiques (hauteur min 38-44px), liste des cartes de plages actives/exceptions et raccourcis de création rapide (Matin 08h-12h, Après-midi 14h-18h, Journée 08h-17h).
  - **Desktop (≥ 768px)** : Grille 7 colonnes standard conservée dans un conteneur responsive.
- **Formulaire de plage `AvailabilityPageComponent`** :
  - Suppression des réinitialisations parasites (`resetRuleForm()`) lors des sélections de plages/jours.
  - Liaison bidirectionnelle réactive `onFormWeekdayChange` et `onFormValidFromChange` avec attribut `[selected]="day === formWeekday()"` sur l'élément HTML `<select>`.

