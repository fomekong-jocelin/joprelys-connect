# Changelog

Tous les changements notables du projet doivent être documentés ici.

Le format suit l'esprit de Keep a Changelog et le versioning suit Semantic Versioning.

## [Unreleased]

### Added

- Implémentation complète de la User Story [STORY-0801](file:///C:/MES-APPLICATIONS/joprelys-connect/docs/ai/tickets/STORY-0801-espace-patient.md) (Espace patient sécurisé et historique personnel) :
  - **Backend** : Création des endpoints d'authentification OTP patients (`/api/public/patient/auth/otp` et `verify`), du contrôleur sécurisé patient (`/api/patient/me`), de la méthode de génération de jeton JWT patient et gestion du multi-tenant avec Hibernate en mode natif.
  - **Frontend** : Création de `PatientLoginComponent` (formulaire double étape), `PatientDashboardComponent` (orchestration de l'espace patient), et des sous-composants réutilisables `PatientProfileCardComponent` et `PatientVisitsListComponent` dans un grid responsive respectant [DESIGN.md](file:///C:/MES-APPLICATIONS/joprelys-connect/DESIGN.md).
- Création du fichier de Design System centralisé [DESIGN.md](file:///C:/MES-APPLICATIONS/joprelys-connect/DESIGN.md) à la racine pour standardiser les tokens (couleurs, typographie, espacements, radius) et les règles UI (Tailwind CSS v4 CSS-first).
- Cadrage et raffinement de la première User Story [STORY-0801](file:///C:/MES-APPLICATIONS/joprelys-connect/docs/ai/tickets/STORY-0801-espace-patient.md) (Espace patient sécurisé et historique personnel) avec rédaction de sa spécification fonctionnelle et de son architecture technique.
- Mise à jour de `VerificationComponent` (page publique de vérification d'authenticité) pour prendre en compte et afficher correctement les statuts révoqués (`REVOQUE` / `REVOKED`) et annulés (`ANNULE` / `CANCELLED`) du document médical avec des styles et des libellés adaptés (STORY-0602 / STORY-0603).
- Cadrage initial de l'`EPIC-0008` (Portail Patient & Consentement) avec documentation de l'objectif, du périmètre et rédaction des 4 user stories associées (STORY-0603).
- Refactoring majeur de l'écran de détail du patient `PatientDetailComponent` avec un design à 3 onglets (Fiche Patient, Dossier Médical, Journal d'Audit) conforme à Material Design 3 pour éliminer la surcharge cognitive.
- Déplacement des boutons d'actions principales ("Démarrer la consultation", "Retour") en haut de la fiche dans un en-tête structuré et responsive.
- Intégration de l'affichage du journal d'audit et sécurité en timeline sous forme collapsible dans le détail du patient unique (STORY-0702 / EPIC-0007).
- Service d'API Angular `AuditApiService` et clés d'internationalisation FR/EN pour le support multilingue de la traçabilité (STORY-0702).
- Tests unitaires et d'autorisation d'affichage basés sur les rôles de l'utilisateur actif (STORY-0702).
- Implémentation du module de traçabilité et logs d'audit (backend) (STORY-0701 / EPIC-0007).
- Exposition des API REST sécurisées `GET /api/audit/patients/{patientId}` et `GET /api/audit/organizations/{organizationId}` avec isolation multi-tenant stricte (STORY-0701).
- Ajout d'écouteurs d'événements Spring Security pour journaliser automatiquement les connexions et échecs d'authentification (STORY-0701).
- Intégration d'audit logs automatiques lors de la création/consultation de patients et d'actions sur les documents médicaux (génération, téléchargement, révocation, annulation) (STORY-0701).
- Ajout de la table de données `audit_logs` indexée sous Flyway migration V10 (STORY-0701).
- Mise à niveau du kit de gouvernance IA, Scrum et Architecture vers la version v0.3.8 (TICKET-0108).
- Ajout de standards de design system Tailwind CSS v4 CSS-first et d'arrondis sobres (4px-6px) sous `docs/standards/DESIGN-SYSTEM-STANDARDS.md` et `docs/standards/UI-RADIUS-AND-SHADOW-STANDARDS.md` (TICKET-0108).
- Ajout de standards d'architecture SOLID et limitation stricte à 500 lignes par classe et 40 lignes par méthode sous `docs/standards/ARCHITECTURE-SOLID-RESPONSIBILITY-STANDARDS.md` (TICKET-0108).
- Préservation et isolation des fichiers de données projet (`VERSION`, `CHANGELOG.md`, `PROJECT-TRACKING.md`, `DELIVERY-DASHBOARD.md`, `VERSION-MATRIX.md`) (TICKET-0108).
- Implémentation frontend de l'écran de révocation et d'annulation de documents médicaux pour les cliniciens (STORY-0604 / EPIC-0006).
- Enrichissement de la réponse `ConsultationResponse` sur le backend pour propager les identifiants et statuts de documents sans requêtes N+1 (STORY-0604).
- Ajout du bouton d'action et d'une modale de confirmation interactive avec radio-boutons de type d'action et motif d'audit sur `PatientDetailComponent` (STORY-0604).
- Ajout de 4 tests unitaires frontend dans `patient-detail.component.spec.ts` validant le cycle de révocation/annulation (STORY-0604).
- Implémentation backend de la révocation et de l'annulation de documents médicaux (STORY-0603 / EPIC-0006).
- Ajout des endpoints sécurisés `PATCH /api/documents/{id}/revoke` (statut → `REVOQUE`) et `PATCH /api/documents/{id}/cancel` (statut → `ANNULE`), réservés aux rôles `MEDECIN` et `ADMIN_CLINIQUE` (STORY-0603).
- Ajout des champs de traçabilité `revoked_at`, `revoked_by_user_id`, `revocation_reason` dans la table `medical_documents` via migration Flyway `V9__add_revocation_to_medical_documents.sql` (STORY-0603).
- Ajout de la méthode métier `revoke()` dans `MedicalDocumentEntity` pour centraliser la logique de changement de statut avec audit (STORY-0603).
- Ajout des DTOs `RevokeDocumentRequest` (motif obligatoire ≤ 500 caractères) et `DocumentStatusResponse` (métadonnées d'audit sans données médicales) (STORY-0603).
- Ajout de 5 tests d'intégration dans `DocumentRevocationControllerTest` couvrant révocation, annulation, double révocation (409), rôle non habilité (403) et accès anonyme (401) (STORY-0603).
- Implémentation backend de la gestion du personnel clinique (STORY-0104 / EPIC-0001).
- Ajout de l'API sécurisée `ADMIN_CLINIQUE` `/api/staff` pour lister, inviter, modifier et activer/désactiver les collaborateurs d'une clinique (STORY-0104).
- Ajout de `StaffService` avec isolation multi-tenant par `organizationId`, validation des rôles cliniques, génération du mot de passe temporaire `Jop-XXXXXX` et hash BCrypt (STORY-0104).
- Ajout de `StaffControllerTest` couvrant liste tenant-aware, invitation, doublon email, RBAC, cross-tenant, modification et désactivation bloquant la connexion (STORY-0104).
- Implémentation Angular de la page `/clinic/staff` pour la gestion du personnel clinique, réservée à `ADMIN_CLINIQUE` (STORY-0104).
- Ajout de `StaffApiService`, `StaffManagementComponent` et `StaffManagementComponent` tests pour lister, inviter, modifier, copier le mot de passe temporaire et suspendre/réactiver les collaborateurs (STORY-0104).
- Ajout de l'accès "Équipe clinique" dans le dashboard et des traductions FR/EN associées (STORY-0104).
- Amélioration et refondation du modal de saisie des constantes vitales (design premium, groupements logiques, icônes, et validation client en temps réel avec désactivation du bouton d'enregistrement).
- Backend : Ajout de l'endpoint `GET /api/visits/{id}` requis lors du chargement initial de la saisie de consultation.
- Sécurité : Configuration de Spring Security pour autoriser `/error` publiquement, évitant de transformer les erreurs 404 en 401 Unauthorized.
- Implémentation de la page publique de vérification d'authenticité et intégration du téléchargement de PDF (STORY-0602 / EPIC-0006).
- Création du composant public `VerificationComponent` avec design premium, responsive, support du mode sombre et protection du secret médical (RGPD).
- Ajout de la route anonyme `/verify/:documentId` dans `app.routes.ts`.
- Intégration du bouton "Télécharger PDF" dans la section historique médical du composant `PatientDetailComponent` avec gestion d'erreurs en cas de visite non clôturée.
- Implémentation de la génération et du stockage des documents PDF médicaux lors de la clôture des visites (STORY-0601 / EPIC-0006).
- Ajout de l'entité JPA `MedicalDocumentEntity` et de son repository `MedicalDocumentRepository` avec support multi-tenant via `@TenantId`.
- Ajout du générateur séquentiel `DocumentNumberGenerator` (format `DOC-YYYYMMDD-XXXXXX`) contournant le tenant context via `JdbcTemplate`.
- Ajout du générateur de QR codes `QrCodeGeneratorService` via `zxing`.
- Ajout du générateur de PDF `PdfGeneratorService` via `openpdf` (conversion en format de tableau moderne `PdfPTable` et `PdfPCell`).
- Ajout de `DocumentService` pour l'orchestration de la génération du QR code et du PDF, le stockage sur le disque local, et la persistance en base.
- Intégration de la génération automatique dans `VisitService.closeVisit()`.
- Ajout de `DocumentController` exposant un endpoint sécurisé de téléchargement de PDF `/api/visits/{visitId}/document` (RBAC) et un endpoint public anonyme de vérification `/api/public/documents/{id}/verify`.
- Adaptation de `SecurityConfig` pour ouvrir l'accès public à `/api/public/**`.
- Ajout de tests d'intégration complets dans `DocumentControllerTest` (vérification de la clôture, du téléchargement sécurisé, du cross-tenant, et de la validation publique sans informations sensibles).
- Implémentation du module de consultation médicale backend (STORY-0501 / EPIC-0005).
- Ajout de la migration Flyway `V6__create_consultation_table.sql` créant la table `consultations` (OneToOne avec `visits`, FK vers `users`, `organization_id` multi-tenant, champs `symptoms`, `clinicalExam`, `diagnosis`, `advice`, `followUp`, `status`).
- Ajout de l'entité JPA `ConsultationEntity` avec annotation `@TenantId` Hibernate pour l'isolation multi-tenant et relation ManyToOne lazy vers `UserAccountEntity` (médecin) (STORY-0501).
- Ajout du `ConsultationRepository` avec requête JPQL `findByVisitId()` et `existsByVisitId()` (STORY-0501).
- Ajout du `ConsultationService` exposant un pattern upsert (création ou mise à jour) résolvant le médecin via `UserAccountRepository.findByEmail()` depuis le principal JWT (STORY-0501).
- Ajout du `ConsultationController` exposant `POST /api/visits/{id}/consultation` (rôles MEDECIN, ADMIN_CLINIQUE) et `GET /api/visits/{id}/consultation` (tous les rôles cliniques) (STORY-0501).
- Ajout du `ConsultationControllerTest` avec 11 cas de test couvrant création, upsert, lecture, RBAC (403), validation Bean (400), visite inexistante (404), sans token (401), isolation multi-tenant cross-tenant (404) et visite clôturée (400) (STORY-0501).
- Implémentation du module de prescription médicale backend (STORY-0502 / EPIC-0005).
- Ajout de la migration Flyway `V7__create_prescription_table.sql` créant les tables `prescriptions` (OneToOne avec `consultations`, @TenantId) et `prescription_items` (1-N médicaments par prescription) (STORY-0502).
- Ajout de `PrescriptionEntity`, `PrescriptionItemEntity`, `PrescriptionRepository` (pattern upsert avec `orphanRemoval`) (STORY-0502).
- Ajout du `PrescriptionService` et du `PrescriptionController` exposant `POST/GET /api/consultations/{id}/prescription` (rôles MEDECIN/ADMIN_CLINIQUE pour la saisie, tous les rôles cliniques + PHARMACIEN pour la lecture) (STORY-0502).
- Ajout du `PrescriptionControllerTest` avec 9 cas de test (création, upsert, lecture, RBAC 403, liste vide 400, 404, 401, isolation multi-tenant) (STORY-0502).
- Ajout de `ConsultationHistoryController` exposant `GET /api/patients/{id}/consultations` (liste paginée des consultations d'un patient, rôles MEDECIN/ADMIN_CLINIQUE/INFIRMIER) (STORY-0504 backend).
- Ajout de `findByPatientIdOrderByCreatedAtDesc()` dans `ConsultationRepository` et `getConsultationsByPatientId()` dans `ConsultationService` (STORY-0504 backend).
- Implémentation du module consultation Angular (STORY-0503 / EPIC-0005).
- Ajout de `consultation.models.ts` (interfaces Consultation, Prescription, PrescriptionItem, SaveConsultationRequest, SavePrescriptionRequest) (STORY-0503).
- Ajout de `ConsultationApiService` avec 5 méthodes HTTP (saveConsultation, getConsultation, savePrescription, getPrescription, getPatientConsultations) (STORY-0503).
- Ajout du `ConsultationComponent` standalone (route `/clinic/consultation/:visitId`) : formulaire réactif complet (symptoms, clinicalExam, diagnosis, advice, followUp) + FormArray prescription avec lignes dynamiques ajoutables/supprimables + affichage des constantes vitales en lecture seule (STORY-0503).
- Ajout du bouton "Démarrer la consultation" dans le drawer du tableau de bord, accessible aux rôles MEDECIN et ADMIN_CLINIQUE, naviguant vers l'écran de consultation (STORY-0503).
- Ajout de la section accordéon "Historique Médical" dans `PatientDetailComponent` : chargement lazy des consultations passées avec date, diagnostic, médecin ; spinner ; message vide (STORY-0504 frontend).

### Fixed

- Correction de la résolution du nom de l'acteur (`actorName`) dans l'API et la timeline du journal d'audit de sécurité (STORY-0702).
- Correction de la référence de clé étrangère dans `V6__create_consultation_table.sql` : `REFERENCES user_accounts(id)` → `REFERENCES users(id)` (nom réel de la table défini dans `V1__create_users_and_auth_audit.sql`) (STORY-0501).

- Implémentation du module de gestion des visites et de la file d'attente active (STORY-0401).
- Ajout du script de migration Flyway de création de la table SQL `visits` rattachée au patient et à l'organisation (STORY-0401).
- Ajout de l'entité JPA `VisitEntity` avec relation FetchType.LAZY pour la performance mémoire et liaison multi-tenant (STORY-0401).
- Ajout du service `VisitNumberGenerator` générant des numéros de visites uniques `VIS-YYYYMMDD-XXXXXX` sans collision inter-tenant (STORY-0401).
- Ajout du service métier `VisitService` et du contrôleur REST `VisitController` exposant les endpoints d'ouverture, de liste et de clôture de visites (STORY-0401).
- Ajout de l'Exception Handler global pour `ResponseStatusException` dans `AuthExceptionHandler` afin de propager proprement les détails d'erreurs d'API sous format ProblemDetail (STORY-0401).
- Ajout de l'intégration Angular `VisitApiService`, de la boîte de dialogue d'ouverture de visite sur `PatientDetailComponent` et du tableau/cartes de file d'attente active sur le tableau de bord clinique (STORY-0401).
- Implémentation du Dossier Patient Unique (DPU) et de l'enregistrement de patients (STORY-0301).
- Ajout de la table SQL `patients` avec contraintes et indexes pour optimiser la recherche (STORY-0301).
- Ajout des APIs d'enregistrement, de recherche et de détails des patients (/api/patients) (STORY-0301).
- Ajout des composants Angular `PatientListComponent`, `PatientFormComponent` et `PatientDetailComponent` intégrant la charte graphique, i18n, thème et mobile-first (STORY-0301).
- Implémentation de l'isolation logique multi-tenant via `@TenantId` d'Hibernate 6, `TenantContext` thread-local et `TenantIdentifierResolver` (STORY-0301).
- Ajout du service de génération d'identifiants séquentiels `PatientNumberGenerator` par jour pour le DPU et le numéro local (STORY-0301).
- Amélioration de la gestion globale des exceptions de validation d'API via `AuthExceptionHandler` pour renvoyer des messages d'erreurs de champs détaillés, et intégration côté frontend pour les formulaires patients et organisations.
- Implémentation de la structure de clinique pilote multi-tenant de base (STORY-0201).
- Ajout de la table SQL `organizations` et clé étrangère `organization_id` associée sur la table `users` (STORY-0201).
- Ajout des APIs d'administration des organisations (/api/organizations) réservées au rôle `ADMIN_JOPRELYS` (STORY-0201).
- Ajout du blocage d'authentification pour les utilisateurs de cliniques désactivées (statut `INACTIVE`) (STORY-0201).
- Ajout de l'écran d'administration Angular de gestion et liste de cliniques pilotes (/organizations) (STORY-0201).
- Implémentation du contrôle d'accès basé sur les rôles (RBAC) de bout en bout (STORY-0102).
- Ajout de l'autorisation d'accès par méthode Spring Security (@EnableMethodSecurity) et d'endpoints de test restrictifs (/api/clinic/*) (STORY-0102).
- Ajout du guard Angular de permissions (role.guard.ts) et de l'écran d'accès refusé (unauthorized.component.ts) (STORY-0102).
- Ajout du Tableau de Bord clinique dynamique affichant les menus selon le rôle (dashboard.component.ts) (STORY-0102).
- Mise en place d'une gouvernance IA centralisée.
- Ajout d'un workflow obligatoire pour Codex, Gemini, Claude Code et autres IA.
- Ajout d'un modèle de ticket actionnable avec cases à cocher.
- Ajout d'un fichier de suivi global `PROJECT-TRACKING.md`.
- Ajout d'un fichier de références techniques.
- Ajout d'une couche Chef de projet / Scrum / Delivery.
- Ajout d'un guide de capacité sprint.
- Ajout d'un guide d'estimation par story points et profils.
- Ajout d'un modèle de dashboard delivery.
- Ajout de templates Epic, User Story, Task, Sprint Plan, Timesheet et Weekly Report.
- Ajout de `TICKET-0101` pour la connexion/deconnexion securisees.
- Ajout de l'API backend `/api/auth/login` et `/api/auth/logout` avec JWT, BCrypt, revocation en memoire et audit des tentatives.
- Ajout des migrations Flyway `users` et `auth_audit_events`.
- Ajout de l'ecran Angular de connexion, du stockage `sessionStorage` et de l'intercepteur Bearer pour les requetes `/api/*`.
- Ajout de tests unitaires backend JWT/auth et de tests Angular login/intercepteur.

### Changed

- Reprise mobile-first de l'écran Angular de gestion des cliniques pilotes : shell applicatif commun, header/footer persistants, thème light par défaut, composants UI réutilisables et affichage liste ou formulaire exclusif (STORY-0201).
- Renforcement du `SKILL.md` avec une règle anti-ticket isolé.
- Extension explicite aux projets Spring Boot, Angular et Flutter.
- Extension du template de ticket avec estimation, profil recommandé, sprint, reviewer et suivi du temps.
- Extension du seeder d'utilisateurs (`AdminUserSeeder`) pour initialiser également les profils médecin (`medecin@...`) et pharmacien (`pharmacien@...`) de test.
- Migration de l'outil de build backend de Gradle vers Maven (TICKET-0104).
- Intégration de Tailwind CSS v4 dans le projet frontend Angular (TICKET-0104).
- Refonte de l'interface de connexion et de session active selon le style épuré "Innerly" (TICKET-0106).
- Intégration de la charte graphique et des polices Montserrat/Inter de Joprelys HealthTech (TICKET-0106).

### Fixed

- Correction du chargement de la file d'attente active du dashboard clinique et chargement des constantes avec les visites actives pour permettre la saisie fiable des constantes après ouverture de visite (STORY-0402).
- Conception et implémentation d'un volet latéral de détails (Drawer / Slide-over) pour la file d'attente active du tableau de bord : affichage complet et aéré des constantes de tri (icônes et cartes associées) et du motif de visite. Ce volet glisse de la droite sur grand écran et du bas vers le haut (Bottom Sheet scrollable) sur mobile (STORY-0402).
- Correction de l'erreur de DataSource PostgreSQL manquante au démarrage du backend, et initialisation de l'administrateur système conforme aux standards de production avec SLF4J et contrôle d'activation (TICKET-0102).
- Configuration du proxy de développement Angular pour rediriger les requêtes `/api/*` vers le backend Spring Boot (TICKET-0103).
- Amélioration des contrastes de couleurs des textes et liens d'action en mode sombre sur le tableau de bord pour la conformité WCAG AA (TICKET-0107).

### Security

- Ajout de références obligatoires OWASP Top 10, OWASP ASVS, OWASP MASVS et OWASP API Security.
- Secret JWT obligatoire via configuration, sans valeur secrete par defaut.
- Erreur de connexion generique pour reduire le risque d'enumeration de comptes.

## [0.3.0] - 2026-07-01

### Added

- Ajout de la gouvernance Semantic Versioning.
- Ajout du fichier racine `VERSION`.
- Ajout de `docs/release/SEMANTIC-VERSIONING.md`.
- Ajout de `docs/release/RELEASE-WORKFLOW.md`.
- Ajout de `docs/release/VERSION-MATRIX.md`.
- Ajout des templates de release note et décision de version.

### Changed

- Mise à jour des instructions IA pour imposer l'analyse PATCH / MINOR / MAJOR.
- Extension des templates ticket, epic, story, task et sprint avec l'impact version.

## [0.2.0] - 2026-07-01

### Added

- Version complète Engineering + Scrum Governance.

## [0.1.0] - 2026-06-29

### Added

- Première version du kit documentaire IA.
