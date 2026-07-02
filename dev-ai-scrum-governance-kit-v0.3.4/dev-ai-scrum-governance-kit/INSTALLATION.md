# Installation du kit

## 1. Copier le kit

Copier tous les fichiers à la racine du dépôt projet.

```bash
cp -R dev-ai-scrum-governance-kit/* /chemin/vers/projet/
```

## 2. Configurer les IA

### Codex / agent générique

Demander explicitement :

```text
Lis d'abord AGENTS.md, SKILL.md, PROJECT-MANAGER-SKILL.md puis applique le workflow adapté.
```

### Claude Code

Claude lit généralement `CLAUDE.md`. Le fichier pointe vers les règles obligatoires.

### Gemini CLI

Gemini lit généralement `GEMINI.md`. Le fichier contient les références `@./...`.

## 3. Créer les premiers fichiers projet

À minima :

```text
docs/pm/sprints/SPRINT-0001.md
docs/pm/reports/WEEKLY-YYYY-MM-DD.md
docs/ai/tickets/TICKET-0001-governance.md
VERSION
docs/release/VERSION-MATRIX.md
```

## 4. Adapter les commandes de test

Dans `docs/ai/WORKFLOW-IA.md`, remplacer les commandes génériques par celles du projet :

```bash
# Backend
./mvnw test
./mvnw clean verify

# Angular
npm run lint
npm run test
npm run build

# Flutter
flutter analyze
flutter test
```

## 5. Règle d'exploitation

Ne pas demander uniquement :

```text
Fais cette feature.
```

Demander plutôt :

```text
Analyse la demande, découpe si elle est macro, crée les tickets, estime par profil, puis implémente uniquement la première tâche prête selon le workflow.
```


## Standards projet à respecter

- Backend Spring Boot : Maven uniquement. Vérifier la présence de `pom.xml` et `mvnw`. Ne pas initialiser de projet Gradle.
- Frontend Angular : Tailwind CSS uniquement. Ne pas installer Angular Material. Vérifier l’absence de `@angular/material` dans `package.json`.
- Spring Boot : utiliser `src/main/resources/application.yml`, pas `application.properties`. Ajouter les profils YAML nécessaires (`application-dev.yml`, `application-test.yml`, `application-prod.yml`).
- Angular : créer ou vérifier `proxy.conf.json` à la racine du frontend et le déclarer dans `angular.json` avec `proxyConfig`.

## Vérification Frontend/Mobile

Avant de démarrer un projet Angular ou Flutter avec ce kit, vérifier que les standards suivants existent ou sont planifiés dans les premiers tickets :

- design system centralisé ;
- thème `light` et thème `dark` ;
- composants/widgets partagés ;
- i18n minimum `fr` / `en` ;
- configuration centrale du nom de l’application, logo, baseline, assets et paramètres publics.

Les exemples de départ sont disponibles dans :

```text
docs/templates/angular/
docs/templates/flutter/
docs/standards/FRONTEND-MOBILE-STANDARDS.md
```

## Documentation First

Créer le dossier suivant dans chaque projet au besoin :

```text
docs/features/
```

À chaque nouvelle feature, démarrer par :

```text
docs/features/<feature-id>/FUNCTIONAL-SPEC.md
docs/features/<feature-id>/TECHNICAL-DESIGN.md
```

Utiliser les modèles fournis dans :

```text
docs/templates/documentation/
```
