# AI Engineering + Scrum Governance Kit

Kit complet de gouvernance pour projets **Spring Boot / Angular / Flutter** avec intervention IA, qualité technique, pilotage Scrum, capacité d'équipe, estimation, suivi de sprint et reporting.

> Date : 2026-07-01  
> Objectif : éviter les tickets flous, les régressions, les dérives non mesurées et les livraisons sans preuve.

## À copier à la racine du projet

```text
AGENTS.md
SKILL.md
PROJECT-MANAGER-SKILL.md
CLAUDE.md
GEMINI.md
docs/
  ai/
  pm/
  qa/
  release/
```

## Rôle des fichiers principaux

| Fichier | Rôle |
|---|---|
| `AGENTS.md` | Point d'entrée commun pour Codex, Claude Code, Gemini, Copilot ou autre IA |
| `SKILL.md` | Règles d'ingénierie logicielle : architecture, sécurité, tests, qualité, anti-régression |
| `PROJECT-MANAGER-SKILL.md` | Règles chef de projet / Scrum Master / Delivery Manager |
| `docs/ai/README-IA.md` | Source de vérité IA du projet |
| `docs/ai/WORKFLOW-IA.md` | Workflow obligatoire pour toute intervention IA |
| `docs/pm/SCRUM-WORKFLOW.md` | Processus Scrum adapté à une petite équipe dev |
| `docs/pm/CAPACITY-PLANNING.md` | Calcul de capacité, disponibilité et charge sprint |
| `docs/pm/ESTIMATION-GUIDE.md` | Story points, jours homme, profils junior/intermédiaire/senior |
| `docs/pm/TEAM-PROFILES.md` | Niveaux de profils, autonomie, types de tâches, pondération |
| `docs/pm/DELIVERY-DASHBOARD.md` | Tableau de pilotage prévu/réalisé/dérive/blocages |
| `docs/release/SEMANTIC-VERSIONING.md` | Règles SemVer : PATCH, MINOR, MAJOR, breaking changes, tags |
| `docs/release/RELEASE-WORKFLOW.md` | Workflow de préparation de release, changelog, tag, rollback |
| `docs/release/VERSION-MATRIX.md` | Versions par module backend, web, mobile, API, DB |

## Principe central

Une IA ne doit jamais traiter une demande comme un ticket isolé.

Elle doit déterminer si la demande relève de :

1. **Engineering** : implémentation, bug, refactoring, sécurité, tests ;
2. **Project Management** : cadrage, découpage, estimation, capacité, sprint, suivi ;
3. **QA / Review** : revue technique, régression, DoD, tests, qualité ;
4. **Architecture** : décision structurante nécessitant un ADR ;
5. **Release Management** : version, changelog, tag Git, hotfix, release candidate, rollback.

## Utilisation rapide

### Demande macro

```text
Utilise PROJECT-MANAGER-SKILL.md. Découpe cette demande en EPIC, user stories et tâches estimées par profil.
```

### Demande de développement

```text
Utilise AGENTS.md et SKILL.md. Crée ou mets à jour le ticket, analyse l'existant, implémente avec tests et mets à jour le suivi.
```

### Préparation sprint

```text
Utilise docs/pm/CAPACITY-PLANNING.md et docs/pm/SPRINT-PLANNING.md. Prépare le sprint selon la capacité réelle de l'équipe.
```

### Analyse de dérive

```text
Compare estimation, temps passé, blocages, qualité de sortie et réouverture du ticket. Dis si la cause vient du cadrage, de l'estimation, du profil, d'un blocage ou d'une sous-performance.
```

## Règle de mesure

Les story points mesurent la complexité et l'incertitude d'une tâche.  
Le temps passé sert à calibrer la capacité, pas à humilier ou comparer mécaniquement les développeurs.

## Installation

Voir `INSTALLATION.md`.


## Semantic Versioning

Le kit impose maintenant une gouvernance de version.

Pour toute livraison, l'IA doit déterminer si le changement implique :

- **PATCH** : correction rétrocompatible ;
- **MINOR** : nouvelle fonctionnalité rétrocompatible ;
- **MAJOR** : breaking change API, DB, auth, contrat mobile/web ou comportement métier cassant.

Fichiers concernés :

```text
VERSION
docs/release/SEMANTIC-VERSIONING.md
docs/release/RELEASE-WORKFLOW.md
docs/release/VERSION-MATRIX.md
docs/release/templates/TEMPLATE-release-note.md
docs/release/templates/TEMPLATE-version-decision.md
```

Prompt type :

```text
Prépare la release : analyse les tickets inclus, applique SemVer, propose la prochaine version, mets à jour le changelog, prépare la release note et indique le tag Git.
```
