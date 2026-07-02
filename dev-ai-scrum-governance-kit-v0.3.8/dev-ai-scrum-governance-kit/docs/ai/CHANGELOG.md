# Changelog

Tous les changements notables du projet doivent être documentés ici.

Le format suit l'esprit de Keep a Changelog et le versioning suit Semantic Versioning.

## [Unreleased]

## [0.3.8] - 2026-07-02

### Added

- Ajout d’un `.gitignore` racine standard pour projets Spring Boot / Angular / Flutter.
- Ajout du standard `docs/standards/GITIGNORE-STANDARDS.md`.
- Ajout des templates `.gitignore` par stack :
  - Spring Boot Maven ;
  - Angular Tailwind CSS v4 ;
  - Flutter ;
  - projet combiné Spring + Angular + Flutter.

### Changed

- Renforcement des workflows IA, QA et templates tickets pour vérifier le `.gitignore`.
- Ajout d’une règle empêchant de versionner secrets, caches, artefacts de build et fichiers locaux.




## [0.3.7] - 2026-07-02

### Added

- Ajout du standard `docs/standards/ARCHITECTURE-SOLID-RESPONSIBILITY-STANDARDS.md`.
- Ajout des règles strictes SOLID, séparation des responsabilités et backend maître.
- Ajout de règles sur services/use cases, interfaces/implémentations, généricité, héritage et abstractions.
- Ajout du template `docs/templates/spring/ARCHITECTURE-PACKAGE-STRUCTURE.md`.

### Changed

- Renforcement des workflows IA, QA, tickets et templates Scrum pour contrôler la logique métier, la taille des classes et la séparation backend/front.



## [0.3.6] - 2026-07-02

### Added

- Ajout du standard `docs/standards/UI-RADIUS-AND-SHADOW-STANDARDS.md`.
- Ajout d’une règle stricte sur les arrondis sobres pour Angular et Flutter.
- Ajout d’une règle de profondeur par ombres légères plutôt que par arrondis excessifs.
- Mise à jour des templates design pour documenter les tokens de radius et shadows.

### Changed

- Renforcement des workflows IA, QA et design pour bloquer les interfaces trop arrondies sans justification.



## [0.3.5] - 2026-07-02

### Added

- Ajout du standard obligatoire **Google DESIGN.md + Tailwind CSS v4** pour les interfaces Angular et Flutter.
- Ajout de `docs/standards/DESIGN-SYSTEM-STANDARDS.md`.
- Ajout de `docs/templates/design/DESIGN.md.example`.
- Ajout du template Angular `tailwind-v4-theme.css.example` basé sur `@theme`.
- Ajout des contrôles design system dans les tickets, stories, tasks, QA, release et documentation technique.

### Changed

- Remplacement de l’ancien exemple Tailwind v3 par une approche Tailwind CSS v4 CSS-first.
- Renforcement des règles de review : `DESIGN.md` obligatoire pour toute UI significative.
- Interdiction explicite de Tailwind v3, `@tailwind base/components/utilities` et `tailwind.config.js` comme source de tokens applicatifs sauf ADR.
- Extension du workflow UI pour vérifier tokens, contrastes, light/dark, i18n, composants réutilisables et mapping Flutter.


## [0.3.4] - 2026-07-02

### Added

- Ajout du standard obligatoire **Documentation First** : documentation fonctionnelle et technique dès le démarrage du développement.
- Ajout de `docs/standards/DOCUMENTATION-FIRST.md`.
- Ajout des templates de documentation : functional spec, technical design, API contract, data model, test plan et user guide.

### Changed

- Renforcement des Definition of Ready / Definition of Done avec documentation obligatoire.
- Mise à jour des workflows IA, Scrum, QA et release pour bloquer les changements non documentés.
- Ajout de la charge documentaire dans la capacité sprint.


## [0.3.3] - 2026-07-02

### Added

- Ajout des standards obligatoires Frontend/Mobile : thème centralisé, composants réutilisables, internationalisation FR/EN, thèmes light/dark et configuration applicative centralisée.
- Ajout de `docs/standards/FRONTEND-MOBILE-STANDARDS.md`.
- Ajout de templates Angular pour `app.config.ts`, thème, Tailwind et fichiers i18n.
- Ajout de templates Flutter pour `AppConfig`, thème, contrôleur de thème et fichiers l10n/ARB.

### Changed

- Renforcement des checklists de review avec des critères bloquants UI/i18n/branding.
- Mise à jour des templates PM pour cadrer les impacts thème, i18n, composants et configuration applicative.

## [0.3.2] - 2026-07-02

### Added

- Ajout du standard obligatoire **Spring Boot configuration YAML** : `application.yml` et profils `application-<profile>.yml`.
- Ajout du standard obligatoire **Angular dev proxy** : `proxy.conf.json` versionné et référencé dans `angular.json`.
- Ajout de templates d’exemple pour `application.yml` et `proxy.conf.json`.

### Changed

- Renforcement des workflows, checklists, templates PM/QA/release pour bloquer `application.properties`, l’absence de proxy Angular et les URLs backend hardcodées côté frontend.



## [0.3.1] - 2026-07-02

### Changed

- Ajout du standard obligatoire **Maven uniquement** pour les backends Spring Boot.
- Ajout du standard obligatoire **Tailwind CSS v4 uniquement** pour les frontends Angular.
- Interdiction d’Angular Material sauf exception documentée par ADR.
- Mise à jour des workflows, checklists, templates Scrum/PM, QA et release pour appliquer ces standards.


### Added

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

### Changed

- Renforcement du `SKILL.md` avec une règle anti-ticket isolé.
- Extension explicite aux projets Spring Boot, Angular et Flutter.
- Extension du template de ticket avec estimation, profil recommandé, sprint, reviewer et suivi du temps.

### Security

- Ajout de références obligatoires OWASP Top 10, OWASP ASVS, OWASP MASVS et OWASP API Security.

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
