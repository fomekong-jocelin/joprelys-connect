# Changelog

Tous les changements notables du projet doivent être documentés ici.

Le format suit l'esprit de Keep a Changelog et le versioning suit Semantic Versioning.

## [Unreleased]


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
- Ajout du standard obligatoire **Tailwind CSS uniquement** pour les frontends Angular.
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
