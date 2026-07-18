# EPIC-0025 — Disponibilités médecins et prise de rendez-vous patient

| Champ | Valeur |
|---|---|
| Type | Epic (demande macro → découpage PM obligatoire, AGENTS.md §4) |
| Statut | READY (cadré, non engagé en sprint) |
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

**Objectif** : poser le socle persistant et sécuritaire du module.

- Critères d'acceptation :
  - Migrations V70 (tables `doctor_availabilities`, `doctor_availability_exceptions`, `appointments`, index tenant + unicité créneau) et V71 (seed permissions RBAC) validées sur PostgreSQL 16 via Testcontainers et sur H2 test.
  - Entités JPA + repositories créés dans `com.joprelys.backend.appointment` (structure `api/application/domain/infrastructure`, `@TenantId`, pattern existant).
  - Permissions `APPOINTMENT_READ`, `APPOINTMENT_WRITE`, `AVAILABILITY_MANAGE` cataloguées et affectées aux rôles `MEDECIN`, `AGENT_ACCUEIL`, `ADMIN_CLINIQUE` (rôle `PATIENT` : accès portail uniquement).
  - `FUNCTIONAL-SPEC.md`, `TECHNICAL-DESIGN.md`, `DATA-MODEL.md`, `API-CONTRACT.md` finalisés.
- Tasks : T-2601.1 finalisation doc (0,5 j) ; T-2601.2 migrations V70/V71 (0,5 j) ; T-2601.3 entités + repositories + tests migration (0,5 j).
- Profil : Senior backend. Reviewer : Tech Lead + référent données.
- Tests : Testcontainers Flyway, validation `ddl-auto: validate`, tests repository.
- Dépendances : aucune. **Cette story débloque toutes les autres.**

### STORY-2602 — Gestion des disponibilités médecin (8 SP — 3,0 j Senior)

**Objectif** : le médecin (ou l'admin) définit ses plages récurrentes et ses indisponibilités ; le backend génère les créneaux.

- Backend : endpoints `/api/availabilities` (CRUD règles + exceptions), service de génération de créneaux (règles − exceptions − créneaux réservés), logique métier côté service (SOLID, controller sans logique).
- Frontend : page `clinic/availability` « Mes disponibilités » (médecin), composants partagés `shared/ui` (grille hebdo, plages horaires), i18n FR/EN, light/dark, radius ≤ 8 px selon `DESIGN.md`.
- Critères d'acceptation : un médecin crée/modifie/supprime ses plages ; les indisponibilités ponctuelles masquent les créneaux ; les RDV existants ne sont jamais supprimés silencieusement (conflit → action explicite clinique) ; aucun texte en dur ; aucun composant > 500 lignes.
- Tests : unitaires service de génération (cas limites : chevauchements, exception, minuit), intégration MockMvc AuthN/AuthZ, Vitest composants.
- Profil : Senior full-stack. Reviewer : Lead Backend + Lead Frontend.
- Dépendance : STORY-2601.

### STORY-2603 — Prise de rendez-vous patient (8 SP — 3,5 j Senior)

**Objectif** : depuis le portail patient, consulter l'annuaire des médecins, voir les créneaux libres, réserver et annuler.

- Backend : `/api/patient/appointments/**` (annuaire médecins, créneaux disponibles, réservation transactionnelle avec contrainte d'unicité anti double réservation, annulation selon délai configurable, liste « mes rendez-vous »), résolution patient via `PatientAccessGuardService`.
- Frontend : pages portail `patient/appointments` (annuaire, calendrier/créneaux, confirmation, mes RDV, annulation), composant `shared/ui` calendrier/slot-picker **maison** (aucune lib tierce), dictionnaire i18n `features/appointments` enregistré dans `I18nService.loadLocale()`.
- Critères d'acceptation : double réservation impossible (test de concurrence) ; réservation limitée à l'horizon configurable (défaut 30 j) ; annulation patient possible jusqu'à 24 h avant (configurable) ; un seul RDV actif par patient/médecin/jour ; états chargement/erreur/vide ; accessibilité clavier.
- Tests : concurrence double booking, règles d'annulation, AuthZ (patient ne voit que ses RDV), Vitest parcours.
- Profil : Senior full-stack. Reviewer : Tech Lead + QA.
- Dépendances : STORY-2601, STORY-2602 (créneaux).

### STORY-2604 — Cahier de rendez-vous accueil et conversion en visite (5 SP — 2,0 j Senior)

**Objectif** : l'agent d'accueil consulte l'agenda, réserve pour un patient et transforme le RDV en visite à l'arrivée (lien avec la file d'attente existante).

- Backend : `/api/appointments` (liste par date/médecin/statut, réservation pour un patient, check-in → création `VisitEntity` liée `visit_id`, marquage no-show), statuts `CONFIRMED / CANCELLED_BY_PATIENT / CANCELLED_BY_CLINIC / COMPLETED / NO_SHOW`.
- Frontend : page `clinic/appointments` (agenda jour/semaine, réservation assistée, check-in).
- Critères d'acceptation : le check-in crée une visite cohérente avec la file d'attente existante ; un RDV honoré passe `COMPLETED` ; les absences sont marquables `NO_SHOW`.
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
- **Config** : `joprelys.appointments.*` dans `application.yml` (durée créneau défaut 30 min, horizon 30 j, délai annulation 24 h, rappel J-1) — YAML uniquement.
- **DESIGN.md** : à mettre à jour avec les composants calendrier/slot-picker (arrondis ≤ 8 px, ombres légères, light/dark).

## 6. Risques

| Risque | Mitigation |
|---|---|
| Double réservation en concurrence | Contrainte unique partielle `(doctor_id, start_at)` hors statuts annulés + transaction service + test de concurrence |
| Fuseau horaire | `timestamptz`, fuseau de la clinique côté serveur, formatage local côté front |
| Écrans monolithiques (dette connue) | Découpage en composants < 500 lignes dès le départ |
| Filtrage médecins sur rôle CSV multi-rôles | Réutiliser le pattern `StaffService` existant |
| Tokens patient sans `patient_id` | Résolution serveur via `PatientAccessGuardService` (pattern existant) |
| Régression file d'attente `visit` | Conversion RDV → visite additive, tests d'intégration dédiés |
| Charge sprint | SPRINT-0014 déjà engagé à 34,60 j → engagement en SPRINT-0015 après recalibrage capacité |

## 7. Impact version / SemVer

Bump prévu : **MINOR** (nouvelle fonctionnalité rétrocompatible, aucun breaking change API/DB/auth). Aucune livraison à ce stade : cadrage uniquement.

## 8. Action plan

- [x] Lire la gouvernance (AGENTS.md, SKILL.md, PROJECT-MANAGER-SKILL.md, WORKFLOW-IA.md, PROJECT-TRACKING.md, CHANGELOG.md, review-checklist.md, DOCUMENTATION-FIRST.md, SEMANTIC-VERSIONING.md, DESIGN.md)
- [x] Analyser l'existant backend et frontend
- [x] Créer le ticket EPIC-0025 avec découpage et estimations
- [x] Créer `docs/features/doctor-availability-appointments/FUNCTIONAL-SPEC.md` (initiale)
- [x] Créer `docs/features/doctor-availability-appointments/TECHNICAL-DESIGN.md` (initiale)
- [x] Mettre à jour `docs/ai/PROJECT-TRACKING.md`
- [x] Mettre à jour `docs/ai/CHANGELOG.md`
- [ ] Validation du cadrage par le Product Owner (priorité, périmètre MVP, sprint)
- [ ] Engager STORY-2601 en SPRINT-0015 après recalibrage capacité
- [ ] Compléter `DATA-MODEL.md` et `API-CONTRACT.md` dans STORY-2601
- [ ] Évaluer en STORY-2601 si un ADR est requis (modélisation créneaux/conversion visite)

## 9. Références

- `Cahier_des_charges_Joprelys_Connect_V2.md` (:166 cahier de RDV accueil, :646 table `appointments` cible)
- `docs/features/visite/FUNCTIONAL-SPEC.md` (file d'attente existante)
- `docs/features/patient-portal/FUNCTIONAL-SPEC.md`
- Standards détaillés à appliquer à l'implémentation : `docs/standards/CONFIGURATION-STANDARDS.md`, `FRONTEND-MOBILE-STANDARDS.md`, `DESIGN-SYSTEM-STANDARDS.md`, `ARCHITECTURE-SOLID-RESPONSIBILITY-STANDARDS.md`, `UI-RADIUS-AND-SHADOW-STANDARDS.md`
