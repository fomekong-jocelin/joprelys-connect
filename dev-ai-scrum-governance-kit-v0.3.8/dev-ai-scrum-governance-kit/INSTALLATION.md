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
- Frontend Angular : Tailwind CSS v4 uniquement. Ne pas installer Angular Material. Vérifier l’absence de `@angular/material` dans `package.json`.
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

## Installation Design System / Tailwind v4

Pour un projet Angular ou Flutter avec UI :

1. Créer ou adapter `DESIGN.md` depuis :

```text
docs/templates/design/DESIGN.md.example
```

2. Pour Angular, utiliser Tailwind CSS v4 en CSS-first. Exemple :

```bash
npm install tailwindcss @tailwindcss/postcss
```

3. Créer le CSS global avec :

```css
@import "tailwindcss";
@custom-variant dark (&:where([data-theme="dark"], [data-theme="dark"] *));
@theme {
  --color-primary: #2563eb;
}
```

4. Si `@google/design.md` est utilisé :

```bash
npx @google/design.md lint DESIGN.md
npx @google/design.md export DESIGN.md css-tailwind > src/styles/design-system.theme.css
```

5. Ne pas créer de `tailwind.config.js` pour les tokens applicatifs, sauf ADR.
6. Ne pas installer Angular Material.


## Règle UI obligatoire — Arrondis sobres

Pour tout développement Angular ou Flutter, appliquer `docs/standards/UI-RADIUS-AND-SHADOW-STANDARDS.md`.

Règle courte :
- coins carrés ou légèrement arrondis ;
- 4px à 6px recommandés ;
- 8px maximum sans ADR ;
- pas de style trop arrondi type iPhone ;
- pas de `rounded-full`, `rounded-2xl`, `rounded-3xl` sur cards, formulaires, inputs ou boutons standards ;
- utiliser des ombres légères pour créer la profondeur ;
- documenter la stratégie dans `DESIGN.md`.


## Règle obligatoire — SOLID, responsabilités et backend maître

Toute intervention Spring Boot, Angular ou Flutter doit appliquer `docs/standards/ARCHITECTURE-SOLID-RESPONSIBILITY-STANDARDS.md`.

Règles courtes :
- SOLID doit être respecté à la lettre.
- Le backend est le maître de la vérité métier : validation, décisions, sécurité, persistance et règles critiques.
- Le front web/mobile affiche, collecte et orchestre l’expérience ; il ne porte pas de logique métier complexe ni de vérité critique.
- Les controllers Spring Boot ne contiennent aucune logique métier et ne doivent jamais appeler directement repositories, SQL ou clients externes.
- Les controllers délèguent à des services/use cases ; les services exposés aux controllers doivent avoir un contrat clair et une implémentation dédiée.
- Les composants Angular/Flutter ne doivent pas être monolithiques ; extraire services, facades, use cases, widgets/composants réutilisables.
- Privilégier composition, interfaces et abstractions utiles ; éviter héritage profond et abstractions spéculatives.
- Aucune classe, composant ou widget ne doit dépasser 500 lignes ; alerte dès 300 lignes.


## Règle obligatoire — `.gitignore`

Chaque projet doit avoir un `.gitignore` à la racine, adapté à sa stack réelle.

L’agent doit vérifier ce fichier avant toute livraison et appliquer :

```text
docs/standards/GITIGNORE-STANDARDS.md
```

Règles minimales :

- ne jamais versionner les secrets ;
- ignorer `target/`, `node_modules/`, `dist/`, `build/`, `.dart_tool/`, caches et logs ;
- conserver les fichiers de gouvernance IA ;
- conserver `proxy.conf.json` côté Angular sauf variante locale ;
- adapter le `.gitignore` au projet réel au lieu de copier aveuglément un template.
