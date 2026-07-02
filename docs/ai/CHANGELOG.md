# Changelog

Tous les changements notables du projet doivent être documentés ici.

Le format suit l'esprit de Keep a Changelog et le versioning suit Semantic Versioning.

## [Unreleased]

### Added

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
