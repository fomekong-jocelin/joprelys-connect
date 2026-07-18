# EPIC-0025 — Disponibilités médecins et prise de rendez-vous patient

| Champ | Valeur |
|---|---|
| Type | Epic (demande macro → découpage PM obligatoire, AGENTS.md §4) |
| Statut | IN_PROGRESS (STORY-2601 en revue PR #62 ; STORY-2602 livrée sur branche dédiée, en revue) |
| Priorité | P1 (à confirmer par le Product Owner) |
| Sprint proposé | SPRINT-0015 (SPRINT-0014 déjà chargé à 34,60 j engagés) |
| Story points | 29 SP |
| Estimation | 12,0 j Senior / ~16,0 j Intermédiaire / ~24,0 j Junior encadré |
| Profil recommandé | Senior full-stack + Lead Frontend + QA ; relecture métier référent clinique |
| Reviewer epic | Tech Lead + QA |
| Date de cadrage | 2026-07-18 |
| Documentation | `docs/features/doctor-availability-appointments/` |

## 1. Contexte et constat d'existant

Analyse du dépôt au 2026-07-18 :

- **Aucun module rendez-vous/disponibilité n'existe** (vérifié par grep sur `backend/src/main/java` et `db/migration`, et sur `web/src/app`) : ni entité, ni table, ni endpoint, ni permission, ni écran, ni clé i18n.
- Socle réutilisable et sain :
  - **Portail patient sécurisé** déjà en place : `PatientPortalController` (`/api/patient`, rôle `PATIENT`), authentification OTP, résolution via `PatientAccessGuardService` (lien `globalPatientNumber`).
  - **Annuaire médecins** exploitable : `users.specialty`, `department`, `registration_number` (migration V41) ; `StaffController` (`/api/staff`) et pattern de filtrage du rôle CSV multi-rôles dans `StaffService`.
  - **RBAC en base** extensible (migration V57, `RbacCatalog`) — permissions RDV à créer.
  - **Flyway** : dernière migration **V69** → prochaines : **V70+**.
  - **SMTP transactionnel** opérationnel (e-mails premium HTML, TASK-20260717-PREMIUM-ACCOUNT-EMAILS).
  - **Frontend conforme** : Angular 22 standalone + signals, Tailwind v4 CSS-first, proxy `/api`, `roleGuard`, i18n FR/EN maison, light/dark. **Aucun composant calendrier/date-picker** : à créer dans `shared/ui` (Angular Material interdit).
  - `VisitEntity` = file d'attente walk-in : socle pour matérialiser un RDV honoré (conversion RDV → visite).
- Le Cahier des charges V2 mentionne déjà le « cahier de rendez-vous » de l'agent d'accueil (:166) et une table `appointments` cible (:646) — jamais implémentés.

**Conclusion : faisable, en module greenfield sans régression sur l'existant.**

## 2. Objectif

Permettre aux médecins de publier leurs disponibilités et aux patients de consulter les créneaux libres et de prendre rendez-vous depuis le portail patient, avec cahier de rendez-vous côté accueil et conversion en visite à l'arrivée.

## 3. Périmètre

Inclus (MVP) : disponibilités récurrentes hebdomadaires + exceptions (indisponibilités), génération de créneaux côté backend, annuaire médecins (spécialité/service), réservation/annulation patient, cahier RDV accueil + réservation pour un patient, conversion RDV → visite, e-mails de confirmation/annulation/rappel.

Exclu (V2+) : téléconsultation, SMS, paiement en ligne, liste d'attente, récurrences mensuelles, intégration calendriers externes, réservation cross-clinique, application mobile Flutter.

## 4. Découpage EPIC → User Stories → Tasks

### STORY-2601 — Fondations données, RBAC et contrats (3 SP — 1,5 j Senior)

**Statut : IN REVIEW** (2026-07-18) — livraison complète poussée sur `feature/story-2601-appointment-foundations` (6 commits, PR #62) ; revue indépendante **APPROVE**, reste l'exécution CI verte et la revue Tech Lead + référent données avant fusion.

**Objectif** : poser le socle persistant et sécuritaire du module.

- Critères d'acceptation :
  - [x] Migrations V70 (tables `doctor_availabilities`, `doctor_availability_exceptions`, `appointments`, index tenant + unicité créneau `uq_appointments_doctor_active_slot`) et V71 (seed permissions RBAC, idempotent pattern V64) — validées sur PostgreSQL 16 via `FlywayPostgresqlMigrationTest` (Testcontainers) et compatibles H2 test (aucun index partiel, aucune spécificité PG non portable).
  - [x] Entités JPA + repositories créés dans `com.joprelys.backend.appointment` (structure `api/application/infrastructure` — voir **D1** en §10, `@TenantId`, pattern `VisitEntity`).
  - [x] Permissions `APPOINTMENT_READ`, `APPOINTMENT_WRITE`, `AVAILABILITY_MANAGE` cataloguées (domaine `RENDEZ_VOUS`) et affectées : `MEDECIN` (READ + AVAILABILITY_MANAGE), `AGENT_ACCUEIL` (READ + WRITE), `ADMIN_CLINIQUE` (héritage automatique des 3) ; rôle `PATIENT` inchangé (accès portail uniquement).
  - [x] `FUNCTIONAL-SPEC.md`, `TECHNICAL-DESIGN.md` (initiaux) + `DATA-MODEL.md`, `API-CONTRACT.md` finalisés.
- Tasks : T-2601.1 finalisation doc (0,5 j) ; T-2601.2 migrations V70/V71 (0,5 j) ; T-2601.3 entités + repositories + tests migration (0,5 j).
- Livrables effectifs : T-2601.1 ✅ (commit `8752a9a`) ; T-2601.2 ✅ (commit `cd54446`) ; T-2601.3 ✅ (commits `dcfb808`, `e107210`) ; suivi ✅ (ce fichier, tracking, changelog) ; style ✅ (commit `4d2eadc`, indentation tabulations).
- Profil : Senior backend. Reviewer : Tech Lead + référent données.
- Tests : Testcontainers Flyway (tables, index unique, FK `SET NULL`, permissions rôles), tests repository H2 (`AppointmentRepositoryTest` : persistance, cycle annulation, unicité créneau, isolation tenant).
- Dépendances : aucune. **Cette story débloque toutes les autres.**

### STORY-2602 — Gestion des disponibilités médecin (8 SP — 3,0 j Senior)

**Statut : IN REVIEW** (2026-07-18) — backend + frontend livrés sur `feature/story-2602-doctor-availability` ; revue indépendante **WARNING sans bloquant** (3 avertissements arbitrés, voir **D7**) ; reste l'exécution CI (`cd backend && ./mvnw test` ; `cd web && npm run test && npm run build && npm run i18n:check`).

**Objectif** : le médecin (ou l'admin) définit ses plages récurrentes et ses indisponibilités ; le backend génère les créneaux.

- Critères d'acceptation :
  - [x] Un médecin crée/modifie/désactive ses plages (7 endpoints `/api/availabilities` ; DELETE règle = désactivation logique `active=false`, suppression physique réservée aux exceptions) ; un `MEDECIN` ne gère que ses propres plages (403 sinon), `ADMIN_CLINIQUE` tous les médecins.
  - [x] Les indisponibilités ponctuelles masquent les créneaux (générateur règles actives − exceptions − créneaux réservés, classe pure `AppointmentSlotGenerator`).
  - [x] Les RDV existants ne sont jamais supprimés silencieusement (RM-07 → 409 `AVAILABILITY_CONFLICT` sur modification/désactivation d'une plage ou suppression d'exception couvrant des RDV actifs futurs).
  - [x] Aucun texte en dur (dictionnaire i18n `features/availability` FR/EN enregistré dans `I18nService.loadLocale()`, clés d'erreur par code backend).
  - [x] Aucun composant > 500 lignes (page 385 lignes ; grille hebdo extraite dans `shared/ui/weekly-availability-grid`, calcul de créneaux extrait dans `availability-slots.util.ts`).
- Backend livré : `AvailabilityController` (contrat `API-CONTRACT.md` §2, `@PreAuthorize("hasAnyRole('MEDECIN','ADMIN_CLINIQUE')")`, zéro logique), `AvailabilityService` + `DefaultAvailabilityService`, `AppointmentSlotGenerator` (service pur sans Spring), `AvailabilityApiException` + `AvailabilityExceptionHandler` (`@Order(HIGHEST_PRECEDENCE)` → codes métier garantis au format `ApiErrorResponse`), timezone clinique configurable (**D6**), méthodes repository dérivées ajoutées (aucune migration, aucun changement d'entité), création restreinte aux utilisateurs portant le rôle `MEDECIN` (durcissement revue, `loadDoctor`).
- Frontend livré : page `clinic/availability` « Mes disponibilités » (plages hebdo CRUD, exceptions, aperçu client des créneaux 7 jours), composant partagé `weekly-availability-grid`, service `AvailabilityApiService`, route + entrée menu (`roleGuard`, permission `AVAILABILITY_MANAGE`), `ConfirmationDialogComponent` pour les actions destructives, `TEST-PLAN.md` et `DESIGN.md` mis à jour.
- Tests livrés : `AppointmentSlotGeneratorTest` (9 cas purs : chevauchements, exception à cheval sur minuit, bornes de validité, créneau tronqué, créneau réservé) ; `AvailabilityControllerTest` (11 cas MockMvc : 401/403/404 cross-tenant, 409 OVERLAP/CONFLICT, CRUD nominal, désactivation logique — 20 méthodes de test backend au total) ; specs Vitest page (12 cas) et grille (5 cas).
- Profil : Senior full-stack. Reviewer : Lead Backend + Lead Frontend.
- Dépendance : STORY-2601.

### STORY-2603 — Prise de rendez-vous patient (8 SP — 3,5 j Senior)

**Objectif** : depuis le portail patient, consulter l'annuaire des médecins, voir les créneaux libres, réserver et annuler.

- Backend : `/api/patient/appointments/**` (annuaire médecins, créneaux disponibles, réservation transactionnelle avec contrainte d'unicité anti double réservation, annulation selon délai configurable, liste « mes rendez-vous » — contrat figé dans `API-CONTRACT.md` §4), résolution patient via `PatientAccessGuardService`. Le service `generateSlots(doctorId, from, to)` livré en STORY-2602 est prêt à être consommé.
- Frontend : pages portail `patient/appointments` (annuaire, calendrier/créneaux, confirmation, mes RDV, annulation), composant `shared/ui` calendrier/slot-picker **maison** (aucune lib tierce), dictionnaire i18n `features/appointments` enregistré dans `I18nService.loadLocale()`.
- Critères d'acceptation : double réservation impossible (test de concurrence) ; réservation limitée à l'horizon configurable (défaut 30 j) ; annulation patient possible jusqu'à 24 h avant (configurable) ; un seul RDV actif par patient/médecin/jour ; états chargement/erreur/vide ; accessibilité clavier. **Exigences reportées des revues précédentes** : (1) toute mutation de `AppointmentEntity` passe par l'entité + `save()` — jamais de bulk update JPQL/SQL (invariant `active_start_at` maintenu côté application) ; (2) garde symétrique 409 `AVAILABILITY_CONFLICT` sur création d'exception recouvrant des RDV actifs futurs (**D7**).
- Tests : concurrence double booking, règles d'annulation, AuthZ (patient ne voit que ses RDV), Vitest parcours.
- Profil : Senior full-stack. Reviewer : Tech Lead + QA.
- Dépendances : STORY-2601, STORY-2602 (créneaux).

### STORY-2604 — Cahier de rendez-vous accueil et conversion en visite (5 SP — 2,0 j Senior)

**Objectif** : l'agent d'accueil consulte l'agenda, réserve pour un patient et transforme le RDV en visite à l'arrivée (lien avec la file d'attente existante).

- Backend : `/api/appointments` (liste par date/médecin/statut, réservation pour un patient, check-in → création `VisitEntity` liée `visit_id`, marquage no-show — contrat figé dans `API-CONTRACT.md` §3), statuts `CONFIRMED / CANCELLED_BY_PATIENT / CANCELLED_BY_CLINIC / COMPLETED / NO_SHOW` (enum `AppointmentStatus` livré en STORY-2601).
- Frontend : page `clinic/appointments` (agenda jour/semaine, réservation assistée, check-in).
- Critères d'acceptation : le check-in crée une visite cohérente avec la file d'attente existante ; un RDV honoré passe `COMPLETED` ; les absences sont marquables `NO_SHOW`. **Exigence reportée (D7)** : durcir la détection RM-07 par couverture de fenêtre (et non seule égalité de créneau) pour les RDV à heure libre saisis par l'accueil.
- Tests : intégration conversion RDV → visite, non-régression module `visit`, Vitest.
- Profil : Full-stack intermédiaire à senior. Reviewer : Lead Backend + référent accueil.
- Dépendances : STORY-2601, STORY-2603.

### STORY-2605 — Notifications e-mail, QA end-to-end et documentation finale (5 SP — 2,0 j Senior)

**Objectif** : e-mails transactionnels RDV (confirmation, annulation, rappel J-1 planifié) + recette complète.

- Backend : réutilisation du service SMTP premium existant, templates HTML FR/EN, job planifié de rappel, aucun secret en dur, pas de PII dans les logs.
- QA : parcours E2E (médecin publie → patient réserve → e-mail → accueil check-in → visite), accessibilité, light/dark, recette métier référent clinique.
- Documentation : `USER-GUIDE.md`, mise à jour `CHANGELOG.md`, décision SemVer, release note.
- Profil : Senior full-stack + QA. Reviewer : Tech Lead + QA + référent métier.
- Dépendances : STORY-2603, STORY-2604.

## 5. Impacts identifiés

- **Backend** : nouveau module `appointment` ; `RbacCatalog` ; migrations V70/V71 ; aucun contrat API existant modifié.
- **Frontend** : nouvelles routes `clinic/availability`, `clinic/appointments`, `patient/appointments` (sous `roleGuard`) ; nouveaux composants `shared/ui` (calendrier, slot-picker) ; enregistrement d'un dictionnaire i18n (liste hardcodée dans `I18nService.loadLocale()` — point d'attention).
- **DB** : 3 tables tenantées + index ; migration additive uniquement (rollback = tables inutilisées, aucune destruction).
- **Sécurité** : `@PreAuthorize` par rôle ; isolation tenant `@TenantId` ; patient résolu serveur-side ; rate limiting existant ; données RDV = données de santé indirectes → pas de PII dans logs, audit des actions.
- **Config** : `joprelys.appointments.*` dans `application.yml` (durée créneau défaut 30 min, horizon 30 j, délai annulation 24 h, rappel J-1, **timezone clinique défaut `Africa/Douala`** — D6) — YAML uniquement. **Livré en STORY-2601** (record `AppointmentProperties`), timezone ajoutée en STORY-2602.
- **DESIGN.md** : à mettre à jour avec les composants calendrier/slot-picker (arrondis ≤ 8 px, ombres légères, light/dark). **Fait en STORY-2602** (section « Disponibilités médecin »).

## 6. Risques

| Risque | Mitigation |
|---|---|
| Double réservation en concurrence | **Implémenté (STORY-2601)** : colonne `active_start_at` + index unique `(doctor_id, active_start_at)` + futur service transactionnel + tests d'unicité ; test de concurrence restant en STORY-2603. **Attention** : invariant maintenu côté application uniquement — interdiction de bulk update JPQL/SQL sur `appointments` (exigence STORY-2603/2604) |
| Fuseau horaire | **Implémenté (STORY-2602, D6)** : `timestamptz`, fuseau de la clinique configurable côté serveur (`joprelys.appointments.timezone`), créneaux émis en UTC, formatage local côté front |
| Écrans monolithiques (dette connue) | Découpage en composants < 500 lignes dès le départ (page dispo 385 lignes, grille extraite) |
| Filtrage médecins sur rôle CSV multi-rôles | Réutiliser le pattern `StaffService` existant (STORY-2603 annuaire) ; appliqué dès 2602 (`loadDoctor`, `hasRole("MEDECIN")`) |
| Tokens patient sans `patient_id` | Résolution serveur via `PatientAccessGuardService` (pattern existant) |
| Régression file d'attente `visit` | Conversion RDV → visite additive, tests d'intégration dédiés |
| Charge sprint | SPRINT-0014 déjà engagé à 34,60 j → engagement en SPRINT-0015 après recalibrage capacité |

## 7. Impact version / SemVer

Bump prévu : **MINOR** (nouvelle fonctionnalité rétrocompatible, aucun breaking change API/DB/auth). STORY-2601 : migrations additives + module nouveau. STORY-2602 : nouveaux endpoints `/api/availabilities` + nouvelle page, aucun contrat existant modifié.

## 8. Action plan

- [x] Lire la gouvernance (AGENTS.md, SKILL.md, PROJECT-MANAGER-SKILL.md, WORKFLOW-IA.md, PROJECT-TRACKING.md, CHANGELOG.md, review-checklist.md, DOCUMENTATION-FIRST.md, SEMANTIC-VERSIONING.md, DESIGN.md)
- [x] Analyser l'existant backend et frontend
- [x] Créer le ticket EPIC-0025 avec découpage et estimations
- [x] Créer `docs/features/doctor-availability-appointments/FUNCTIONAL-SPEC.md` (initiale)
- [x] Créer `docs/features/doctor-availability-appointments/TECHNICAL-DESIGN.md` (initiale)
- [x] Mettre à jour `docs/ai/PROJECT-TRACKING.md`
- [x] Mettre à jour `docs/ai/CHANGELOG.md`
- [ ] Validation du cadrage par le Product Owner (priorité, périmètre MVP, sprint)
- [x] Engager STORY-2601 en SPRINT-0015 après recalibrage capacité — **livrée sur branche dédiée, PR #62, revue indépendante APPROVE**
- [x] Compléter `DATA-MODEL.md` et `API-CONTRACT.md` dans STORY-2601
- [x] Évaluer en STORY-2601 si un ADR est requis (modélisation créneaux/conversion visite) — **non requis** : décisions couvrantes documentées en §10 (D1–D6), aucune rupture d'architecture
- [ ] Revue Tech Lead + CI verte de STORY-2601, puis fusion (PR #62)
- [x] Engager STORY-2602 (disponibilités médecin) — **livrée sur branche dédiée, revue indépendante WARNING sans bloquant (D7)**
- [ ] Revue Lead Backend + Lead Frontend + CI verte de STORY-2602, puis fusion
- [ ] Engager STORY-2603 (prise de rendez-vous patient)

## 9. Références

- `Cahier_des_charges_Joprelys_Connect_V2.md` (:166 cahier de RDV accueil, :646 table `appointments` cible)
- `docs/features/visite/FUNCTIONAL-SPEC.md` (file d'attente existante)
- `docs/features/patient-portal/FUNCTIONAL-SPEC.md`
- Standards détaillés à appliquer à l'implémentation : `docs/standards/CONFIGURATION-STANDARDS.md`, `FRONTEND-MOBILE-STANDARDS.md`, `DESIGN-SYSTEM-STANDARDS.md`, `ARCHITECTURE-SOLID-RESPONSIBILITY-STANDARDS.md`, `UI-RADIUS-AND-SHADOW-STANDARDS.md`

## 10. Décisions d'implémentation (STORY-2601 et STORY-2602)

Décisions actées lors des livraisons, à faire valider en revue (détail complet dans `DATA-MODEL.md` §7) :

- **D1 — Structure du module sans `domain/`** : le module `appointment` suit la structure `api/ application/ infrastructure/persistence/` (pattern des modules récents, ex. `visit`). Le `domain/` annoncé dans `TECHNICAL-DESIGN.md` §2 n'est pas créé : aucun usage avéré à ce stade ; la doc est corrigée (`DATA-MODEL.md` §7).
- **D2 — Anti double réservation portable** : la contrainte partielle PostgreSQL `UNIQUE (doctor_id, start_at) WHERE status NOT IN (...)` est remplacée par la colonne technique nullable `active_start_at` (maintenue par l'entité en `@PrePersist/@PreUpdate` : `start_at` si statut actif `CONFIRMED/COMPLETED/NO_SHOW`, `NULL` si annulé) + index unique simple `(doctor_id, active_start_at)`. Justification : aucune migration V1→V69 n'utilise d'index partiel (vérifié par lecture) et la suite H2 doit rester exécutable ; PostgreSQL et H2 autorisent les NULL multiples dans un index unique → les créneaux annulés redeviennent réservables. **Contrepartie** : l'invariant est maintenu par le cycle de vie JPA — toute mutation doit passer par l'entité + `save()` (interdiction de bulk update, exigence reportée sur STORY-2603/2604).
- **D3 — V71 = `permissions` seulement** : pas de seed SQL des `role_permissions` ; les liens rôle → permission sont possédés par `RbacCatalog.systemRoles()` et réappliqués à chaque démarrage par `RbacStore.seedCatalog()` (DELETE + réinsertion des rôles système), un seed SQL serait écrasé.
- **D4 — `AppointmentStatus` enum String** : petit enum persisté via `@Enumerated(EnumType.STRING)`, copie exacte du pattern `DocumentStatus` ; aucun CHECK de statut en base (convention V68).
- **D5 — Statut par défaut `CONFIRMED`** : posé dans le constructeur Java (pattern `status = "EN_COURS"` de `VisitEntity`), doublé du `DEFAULT 'CONFIRMED'` SQL pour les insertions hors JPA.
- **D6 — Fuseau horaire clinique configurable (STORY-2602)** : `AppointmentProperties.timezone` (défaut `Africa/Douala`, surcharge via `JOPRELYS_APPOINTMENTS_TIMEZONE`) ; le générateur de créneaux découpe en dates locales clinique et émet les créneaux en UTC (`timestamptz`). Mono-fuseau par clinique en V1 (zone ouverte du cahier des charges tranchée).
- **D7 — Création d'exception et RDV existants (revue STORY-2602)** : en 2602, la création d'une indisponibilité recouvrant des RDV actifs futurs n'est volontairement pas bloquée (le masquage seul s'applique) — acceptable tant que la réservation n'existe pas. Garde symétrique 409 `AVAILABILITY_CONFLICT` à ajouter en **STORY-2603** ; durcissement de la détection RM-07 par couverture de fenêtre (RDV à heure libre saisis par l'accueil, contrat §3) reporté en **STORY-2604**. Durcissement appliqué dès la revue 2602 : `loadDoctor` exige le rôle `MEDECIN` (CSV multi-rôles, `hasRole`) pour toute création par un admin.

Point de vigilance revue (mentionné en STORY-2601) **levé en STORY-2602** : le filtre `JwtAuthenticationFilter` injecte à la fois `ROLE_<rôle>` et les permissions brutes comme `GrantedAuthority` (vérifié par lecture) — `hasAnyRole('MEDECIN','ADMIN_CLINIQUE')` et `hasAuthority('AVAILABILITY_MANAGE')` fonctionnent tous deux (pattern `StaffController`). Le contrat conserve `hasAnyRole(...)`.
