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
./gradlew test

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
