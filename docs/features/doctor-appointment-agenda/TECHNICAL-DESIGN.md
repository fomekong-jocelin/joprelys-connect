# TECHNICAL-DESIGN — Agenda personnel du médecin

## 1. Architecture

```text
Angular DoctorAppointmentsPage
        |
        v
DoctorAppointmentsApiService
        |
GET /api/doctor/appointments?from&to
        |
DoctorAppointmentController
        |
DoctorAppointmentService
        |
DefaultDoctorAppointmentService
        |
AppointmentRepository + UserAccountRepository
```

Le contrôleur ne contient aucune logique métier. Le service résout l’identité authentifiée, valide la période, vérifie le compte médecin puis transforme les entités en DTO immuables.

## 2. Sécurité

- `@PreAuthorize("hasAuthority('APPOINTMENT_READ') and hasRole('MEDECIN')")` au niveau contrôleur ;
- identité médecin dérivée de `JwtClaims.subject()` et établissement de `JwtClaims.organizationId()` ;
- aucune entrée `doctorId` ;
- résolution du compte par `findByIdAndOrganizationId` ;
- compte désactivé ou non médecin rejeté par `403 DOCTOR_APPOINTMENT_ACCESS_DENIED` ;
- `@TenantId` conserve l’isolation des rendez-vous ;
- réponse API avec données minimales ;
- aucune PII dans les logs.

## 3. Contrat temporel

- `from` et `to` sont des `Instant` obligatoires ;
- intervalle demi-ouvert : `startAt >= from AND startAt < to` ;
- durée maximale : 92 jours ;
- tri croissant par `startAt` ;
- Angular calcule le lundi local puis sérialise en UTC.

## 4. Backend

### DTO

`DoctorAppointmentResponse` :

- `id` ;
- `patientId` ;
- `patientDisplayName` ;
- `patientLocalNumber` ;
- `startAt` ;
- `endAt` ;
- `status` ;
- `reason` ;
- `visitId`.

### Repository

Ajout d’une méthode dérivée demi-ouverte :

```java
findByDoctorIdAndStartAtGreaterThanEqualAndStartAtLessThanOrderByStartAtAsc(
    UUID doctorId, Instant from, Instant to)
```

### Erreurs

- `VALIDATION_ERROR` — période invalide ou trop large ;
- `DOCTOR_APPOINTMENT_ACCESS_DENIED` — compte désactivé, hors établissement ou non médecin.

Le handler réutilise le format `ApiErrorResponse` et le `trace_id` du filtre global.

## 5. Frontend

### Structure

```text
web/src/app/clinic/appointments/
  doctor-appointments.models.ts
  doctor-appointments-api.service.ts
  doctor-appointments-page.component.ts
  doctor-appointments-page.component.spec.ts
```

### État

- `weekStart` ;
- `appointments` ;
- `loading` ;
- `error` ;
- `lastUpdatedAt`.

Les rendez-vous sont regroupés par clé locale `YYYY-MM-DD` dans un `computed`. Les changements de semaine déclenchent un chargement immédiat. Un `interval(30_000)` relance la lecture jusqu’à destruction du composant.

### UI

- shell partagé ;
- barre de navigation semaine précédente / aujourd’hui / suivante ;
- cartes journalières sobres avec radius 4–6 px et ombre légère ;
- statut rendu par badge sémantique ;
- affichage mobile en pile et desktop en grille ;
- focus visible ;
- aucun texte utilisateur en dur.

## 6. Compatibilité et évolution

Aucune migration de base n’est nécessaire. Le futur cahier global de STORY-2604 pourra introduire son propre endpoint professionnel ou extraire une abstraction de requête commune, sans élargir le contrat sécurisé du médecin.

## 7. Observabilité

- pas de journalisation du contenu des rendez-vous ;
- les erreurs utilisent le `trace_id` existant ;
- le frontend affiche un message générique et permet de relancer.

## 8. SemVer

MINOR : nouvel endpoint et nouvelle page rétrocompatibles, sans modification des contrats existants.
