---
name: production-ready-ai-engineering-governance
scope: Spring Boot / Angular / Flutter / CI-CD / Security / Testing / Documentation
---

# Production-Ready AI Engineering Governance

## 0. Règle non négociable

Avant d'exécuter une tâche technique, l'IA doit lire et appliquer :

1. `AGENTS.md`
2. `SKILL.md`
3. `docs/ai/README-IA.md`
4. `docs/ai/WORKFLOW-IA.md`
5. `docs/ai/PROJECT-TRACKING.md`
6. `docs/ai/CHANGELOG.md`
7. `docs/ai/review-checklist.md`
8. le ticket concerné dans `docs/ai/tickets`

Si la demande concerne cadrage, sprint, capacité, estimation ou priorisation, activer aussi `PROJECT-MANAGER-SKILL.md`.

## 1. Posture attendue

L'IA doit raisonner comme un ingénieur senior responsable du produit complet, pas comme un exécutant pressé.

Une tâche n'est terminée que lorsque :

- le ticket est créé ou mis à jour ;
- l'existant est analysé ;
- les impacts sont identifiés ;
- les changements sont minimaux et justifiés ;
- les tests sont ajoutés ou justifiés ;
- les vérifications sont listées ;
- les risques de régression sont évalués ;
- le suivi projet est mis à jour ;
- le changelog est mis à jour si nécessaire.

## Documentation First obligatoire

La documentation fonctionnelle et technique doit démarrer immédiatement avec le développement.

Avant de coder une feature, un bug complexe, une API, un écran Angular, un écran Flutter, une migration ou une configuration, créer ou mettre à jour :

```text
docs/features/<feature-id>/FUNCTIONAL-SPEC.md
docs/features/<feature-id>/TECHNICAL-DESIGN.md
```

Si applicable, créer ou mettre à jour aussi :

```text
docs/features/<feature-id>/API-CONTRACT.md
docs/features/<feature-id>/DATA-MODEL.md
docs/features/<feature-id>/TEST-PLAN.md
docs/features/<feature-id>/USER-GUIDE.md
```

La documentation doit évoluer avec le code : règles métier, contrats API, modèles de données, configuration, i18n, thème, tests, décisions SemVer et changelog.

Une PR/MR doit être bloquée si elle livre un changement sans documentation à jour.

## 2. Règles anti-régression

Avant de modifier du code, inspecter :

- architecture existante ;
- tests existants ;
- contrats API ;
- modèle de données ;
- migrations ;
- sécurité ;
- configuration ;
- logs et observabilité ;
- impacts CI/CD ;
- impacts Flutter/Angular/Spring selon la stack.

L'IA ne doit jamais :

- supprimer du code sans preuve ;
- changer un contrat public sans migration ou versioning ;
- affaiblir une validation ;
- affaiblir l'autorisation ;
- retirer des tests ;
- ignorer un test qui échoue ;
- hardcoder des valeurs d'environnement ;
- introduire des secrets ;
- dupliquer une règle métier ;
- contourner l'architecture existante.

## 3. Architecture commune

### Backend Spring Boot

- **Maven est le standard obligatoire** : utiliser `pom.xml`, `mvnw`, `./mvnw test`, `./mvnw clean verify`. Ne pas créer ni recommander Gradle (`build.gradle`, `settings.gradle`, `./gradlew`) sauf exception existante documentée par ADR.
- **Configuration YAML obligatoire** : utiliser `src/main/resources/application.yml` et les profils `application-<profile>.yml`. Ne pas créer `application.properties`. Toute présence de `application.properties` doit être signalée comme dette ou non-conformité à migrer.
- Respecter le flux `Controller → Application/Service → Domain ← Infrastructure`.
- Ne jamais exposer les entités JPA en réponse API.
- DTO immuables.
- Transactions au niveau service, pas controller.
- Validation aux frontières.
- Erreurs structurées, pas de stack trace exposée.
- Requêtes paramétrées.
- Tests unitaires, slice tests, intégration selon impact.

### Angular

- **Tailwind CSS est le standard obligatoire** pour l’interface.
- **Angular Material est interdit par défaut** : ne pas ajouter `@angular/material`, modules `Mat*`, composants `mat-*`, thèmes Material ou dépendances associées, sauf exception documentée par ADR.
- **Proxy Angular obligatoire** : maintenir un `proxy.conf.json` versionné, référencé dans `angular.json`, et utiliser des URLs relatives côté services (`/api`, `/auth`). Ne pas hardcoder l’URL backend dans les services Angular.
- Composants centrés sur la présentation.
- Services/facades pour logique métier et appels API.
- Guards/interceptors testés.
- Pas de logique métier lourde dans les composants.
- Validation formulaires.
- Protection XSS, pas de bypass sanitizer sans justification.
- Pas de données sensibles dans `localStorage`.

### Flutter

- Architecture feature-first : `domain`, `data`, `presentation`.
- Domain sans dépendance Flutter.
- Providers/use cases testables.
- Données sensibles dans stockage sécurisé.
- `flutter analyze` et `flutter test` obligatoires selon impact.

## 4. Qualité de code

| Élément | Limite dure | Cible |
|---|---:|---:|
| Classe / composant / widget | 500 lignes | 300 lignes |
| Méthode / fonction | 40 lignes | 25 lignes |
| Paramètres | 5 | 3 |
| Complexité cyclomatique | 10 | 5 |
| Profondeur d'imbrication | 3 | 2 |

Toute violation doit être signalée dans le ticket.

## 5. Sécurité minimale

- Pas de secrets dans le code.
- AuthN/AuthZ vérifiées pour les endpoints sensibles.
- Inputs validés.
- Requêtes SQL paramétrées.
- PII masquée dans logs.
- CORS explicite.
- Dépendances scannées si CI/CD disponible.
- Fichiers uploadés validés en type, taille et contenu.

## 6. Tests et vérifications

L'IA doit proposer ou exécuter selon contexte :

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

Si les tests ne sont pas exécutés, la réponse finale doit dire clairement pourquoi.

## 7. Documentation

Mettre à jour :

- ticket ;
- `PROJECT-TRACKING.md` ;
- `CHANGELOG.md` si changement notable ;
- ADR si décision structurante ;
- README ou API docs si impact utilisateur/développeur.

## 8. Interaction avec la couche chef de projet

Si une tâche est macro, floue ou supérieure à 2-3 jours :

1. activer `PROJECT-MANAGER-SKILL.md` ;
2. découper en epic/story/task ;
3. estimer ;
4. identifier le profil recommandé ;
5. vérifier la capacité sprint ;
6. implémenter seulement les tâches prêtes.


## 9. Semantic Versioning et release

Pour tout changement livré, l'IA doit vérifier l'impact version selon `docs/release/SEMANTIC-VERSIONING.md`.

Règles :

- `fix:` rétrocompatible → PATCH.
- `feat:` rétrocompatible → MINOR.
- `BREAKING CHANGE` ou `!` → MAJOR.
- Aucun changement cassant ne doit être caché dans un PATCH ou un MINOR.
- Une migration DB destructive, un changement d'API, de champ, de type, d'auth ou de comportement métier peut imposer un MAJOR.
- La réponse finale doit contenir `Impact version / SemVer` si le changement est livrable.

À chaque release :

- mettre à jour `VERSION` ;
- mettre à jour `docs/ai/CHANGELOG.md` ;
- mettre à jour `docs/release/VERSION-MATRIX.md` si nécessaire ;
- préparer une note de release ;
- proposer le tag Git ;
- documenter le rollback.

## Référence configuration obligatoire

Pour toute intervention Spring Boot ou Angular, appliquer aussi :

```text
docs/standards/CONFIGURATION-STANDARDS.md
```

## Standards obligatoires Frontend Angular et Mobile Flutter

Ces règles s’appliquent à toute création ou modification d’écran Angular ou Flutter.

### Design system et thème

- Lire ou créer `DESIGN.md` avant toute UI significative.
- Appliquer `docs/standards/DESIGN-SYSTEM-STANDARDS.md`.
- Prévoir systématiquement une gestion de thème centralisée.
- Deux thèmes minimum sont obligatoires : `light` et `dark`.
- Les tokens de design doivent être centralisés : couleurs, typographies, espacements, radius, ombres, z-index, breakpoints et états interactifs.
- Les composants ne doivent pas contenir de valeurs visuelles arbitraires dispersées si elles doivent appartenir au design system.
- Le thème doit pouvoir être appliqué globalement et changé sans modifier chaque écran.

### Angular

- Utiliser Tailwind CSS v4 et des composants maison réutilisables.
- Utiliser `@import "tailwindcss"` et `@theme`; ne pas utiliser la syntaxe Tailwind v3.
- Ne jamais utiliser Angular Material sans ADR accepté.
- Prévoir une structure du type :

```text
src/app/core/config/app.config.ts
src/app/core/theme/theme.service.ts
src/app/core/i18n/i18n.service.ts
src/app/shared/ui/
src/assets/i18n/fr.json
src/assets/i18n/en.json
src/assets/branding/
```

- Le mode dark/light doit être piloté par une classe globale ou des variables CSS/Tailwind, pas par des styles dupliqués écran par écran.
- Les chaînes visibles doivent venir des fichiers i18n, jamais être codées en dur dans les templates.
- Le nom de l’application, le logo, le slogan, l’éditeur, les liens publics et les paramètres d’affichage doivent venir d’une configuration centrale.

### Flutter

- Prévoir une structure du type :

```text
lib/core/config/app_config.dart
lib/core/theme/app_theme.dart
lib/core/theme/theme_controller.dart
lib/core/l10n/
lib/shared/widgets/
lib/l10n/app_fr.arb
lib/l10n/app_en.arb
l10n.yaml
```

- Prévoir `ThemeData` light/dark ou équivalent projet.
- Utiliser `AppLocalizations` ou un mécanisme i18n validé avec au minimum `fr` et `en`.
- Ne pas coder en dur les textes utilisateurs dans les widgets.
- Ne pas coder en dur le nom de l’application, le logo, les assets de branding ou les paramètres publics dans les écrans.
- Les widgets partagés doivent être extraits en composants réutilisables plutôt que dupliqués.

### Blocage en review

Une PR/MR doit être bloquée si elle introduit :

- un écran sans intégration au thème central ;
- des textes visibles codés en dur ;
- une absence de traduction `fr` / `en` pour une fonctionnalité utilisateur ;
- un composant dupliqué au lieu d’un composant partagé ;
- un nom d’application, logo, URL ou paramètre public hardcodé dans un écran ;
- un thème light seulement sans stratégie dark ;
- Angular Material sans ADR accepté.

## Standard DESIGN.md / Tailwind v4 / Flutter tokens

Toute intervention UI Angular ou Flutter doit appliquer `docs/standards/DESIGN-SYSTEM-STANDARDS.md`.

Règles strictes :

- `DESIGN.md` est la source de vérité design.
- Si `DESIGN.md` est absent et que l’UI est significative, le créer avant de coder.
- Angular utilise Tailwind CSS v4 uniquement, en mode CSS-first : `@import "tailwindcss"`, `@theme`, `@custom-variant`.
- Ne pas utiliser la syntaxe Tailwind v3 `@tailwind base/components/utilities`.
- Ne pas utiliser `tailwind.config.js` comme source de tokens applicatifs sauf ADR.
- Ne jamais introduire Angular Material sans ADR.
- Flutter doit centraliser les mêmes tokens dans `AppTheme`, `AppDesignTokens` ou équivalent.
- Toute couleur, typo, spacing, radius et règle composant réutilisable doit provenir du design system.
- Les contrastes, focus, light/dark et i18n FR/EN sont bloquants.


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
