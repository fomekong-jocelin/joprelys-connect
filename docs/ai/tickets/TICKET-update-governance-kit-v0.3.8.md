# TICKET-0108 — Mise à jour du kit de gouvernance IA, Scrum et Architecture v0.3.8

## 1. Objectif

Intégrer proprement les nouvelles règles de gouvernance du kit v0.3.8 (standards SOLID, arrondis UI, règles de design system Tailwind CSS v4, validation Maven, i18n et thèmes) dans le projet existant Joprelys Connect sans casser l'existant ni effacer les données spécifiques au projet.

## 2. Contexte

Le projet disposait d'une version antérieure du kit de gouvernance. Une nouvelle version du kit (v0.3.8) a été fournie sous `dev-ai-scrum-governance-kit-v0.3.8`. Cette mise à jour doit renforcer la qualité et standardiser le code front, mobile et back.

## 3. Fichiers analysés

- Anciennes versions à la racine : `AGENTS.md`, `CLAUDE.md`, `GEMINI.md`, `SKILL.md`, `PROJECT-MANAGER-SKILL.md`, `INSTALLATION.md`, `README.md`, `VERSION`.
- Anciens fichiers sous `docs/standards/`, `docs/pm/`, `docs/qa/`, `docs/release/`.
- Fichiers spécifiques au projet : `docs/ai/CHANGELOG.md`, `docs/ai/PROJECT-TRACKING.md`, `docs/pm/DELIVERY-DASHBOARD.md`, `docs/release/VERSION-MATRIX.md`, tickets sous `docs/ai/tickets/`, ADRs sous `docs/ai/adr/`.

## 4. Fichiers créés

- [TICKET-update-governance-kit-v0.3.8.md](file:///C:/MES-APPLICATIONS/joprelys-connect/docs/ai/tickets/TICKET-update-governance-kit-v0.3.8.md) (ce ticket)
- [ARCHITECTURE-SOLID-RESPONSIBILITY-STANDARDS.md](file:///C:/MES-APPLICATIONS/joprelys-connect/docs/standards/ARCHITECTURE-SOLID-RESPONSIBILITY-STANDARDS.md)
- [DESIGN-SYSTEM-STANDARDS.md](file:///C:/MES-APPLICATIONS/joprelys-connect/docs/standards/DESIGN-SYSTEM-STANDARDS.md)
- [GITIGNORE-STANDARDS.md](file:///C:/MES-APPLICATIONS/joprelys-connect/docs/standards/GITIGNORE-STANDARDS.md)
- [UI-RADIUS-AND-SHADOW-STANDARDS.md](file:///C:/MES-APPLICATIONS/joprelys-connect/docs/standards/UI-RADIUS-AND-SHADOW-STANDARDS.md)
- Nouveaux templates de design et de configuration sous `docs/templates/` (ex: `docs/templates/design/DESIGN.md.example`, `docs/templates/angular/tailwind-v4-theme.css.example`, `docs/templates/spring/ARCHITECTURE-PACKAGE-STRUCTURE.md`).

## 5. Fichiers modifiés

- Racine : `.gitignore`, `AGENTS.md`, `CLAUDE.md`, `GEMINI.md`, `INSTALLATION.md`, `PROJECT-MANAGER-SKILL.md`, `README.md`, `SKILL.md`.
- Standards existants : `CONFIGURATION-STANDARDS.md`, `FRONTEND-MOBILE-STANDARDS.md`.
- PM & QA : `CAPACITY-PLANNING.md`, `ESTIMATION-GUIDE.md`, `README-PM.md`, `SCRUM-WORKFLOW.md`, `SPRINT-PLANNING.md`, `TEAM-PROFILES.md`, `QA-REVIEW-WORKFLOW.md`, `RELEASE-WORKFLOW.md`, `SEMANTIC-VERSIONING.md` et les fichiers de template Scrum sous `docs/pm/templates/`.
- Suivi projet : `docs/ai/CHANGELOG.md`, `docs/ai/PROJECT-TRACKING.md`.

## 6. Règles ajoutées

- **SOLID et Responsabilités** : Respect strict du principe SOLID. Séparation des responsabilités entre contrôleurs (aucune logique métier, pas de repository ou de requêtes SQL directes), services (portant les use cases et validations) et couche domaine. Limite stricte de 500 lignes max par classe/composant (alerte à 300) et 40 lignes par méthode.
- **Design System & Tailwind CSS v4** : Tailwind CSS v4 obligatoire avec syntaxe CSS-first (`@import "tailwindcss"` et `@theme`). Interdiction de Tailwind v3 et de `tailwind.config.js` comme source de tokens. Centralisation obligatoire des couleurs, typographies, espacements et breakpoints dans un `DESIGN.md` obligatoire pour toute UI significative.
- **Arrondis sobres** : Coins carrés ou légèrement arrondis recommandés (4px à 6px, 8px max sans ADR) sur cards, formulaires et boutons. Interdiction de `rounded-2xl` ou `rounded-full` sur ces éléments standard.
- **Gouvernance Gitignore** : Interdiction absolue de versionner des secrets ; exclusion systématique des dossiers de build (`target/`, `node_modules/`, `dist/`).

## 7. Règles déjà présentes

- **Maven uniquement** côté backend (Gradle interdit sauf ADR).
- **Format YAML uniquement** (`application.yml`) pour la configuration Spring Boot (properties interdit).
- **Proxy de développement** versionné obligatoire sous `angular.json` et URLs relatives pour les services frontend.
- **Gestion i18n et Thèmes** : Minimum 2 thèmes (light/dark) et support `fr` / `en`.

## 8. Conflits détectés et Décisions prises

- **Conflit de Version** : Le fichier `VERSION` du nouveau kit contenait `0.3.8`, alors que la version actuelle de l'application du projet est `0.4.0` (suite aux versions précédentes).
  * *Décision* : Conserver `0.4.0` dans le fichier `VERSION` du projet, car ce fichier reflète la version réelle de l'application. La version du kit de gouvernance elle-même est notifiée dans les changelogs.
- **Conflit sur les fichiers de données réelles** : `docs/pm/DELIVERY-DASHBOARD.md` et `docs/ai/CHANGELOG.md` du kit étaient vierges/génériques, tandis que le projet contient son historique et ses indicateurs réels de sprints.
  * *Décision* : Ignorer les copies écrasantes de ces fichiers. Les données réelles du projet ont été préservées, et nous avons complété `CHANGELOG.md` et `PROJECT-TRACKING.md` avec de nouvelles entrées décrivant cet upgrade de gouvernance.

## 9. Actions réalisées

1. Création d'une branche dédiée `chore/update-ai-governance-kit`.
2. Analyse comparée des fichiers existants et du kit v0.3.8.
3. Exécution d'un script de copie récursive préservant les fichiers spécifiques au projet :
   * `VERSION`
   * `docs/ai/CHANGELOG.md`
   * `docs/ai/PROJECT-TRACKING.md`
   * `docs/pm/DELIVERY-DASHBOARD.md`
   * `docs/release/VERSION-MATRIX.md`
4. Ajout des nouveaux standards et fichiers templates de design.
5. Mise à jour de `docs/ai/CHANGELOG.md` et `docs/ai/PROJECT-TRACKING.md`.

## 10. Reste à faire

- Aucun (l'intégration automatisée et sélective est entièrement terminée).

## 11. Risques

- *Faible*. Les fichiers de configuration applicatifs, bases de données et code métier n'ont pas été modifiés.

## 12. Tests ou vérifications à exécuter

- Lancer le build complet pour s'assurer qu'aucune modification de document ou de structure n'impacte la compilation.
  ```bash
  mvn clean test-compile
  npm run build -- --configuration development
  ```

## 13. Statut final

Statut : DONE
