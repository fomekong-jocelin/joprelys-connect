# AGENTS.md — Instructions obligatoires pour toute IA

## 1. Lecture obligatoire

Avant toute action, lire obligatoirement :

- `SKILL.md`
- `PROJECT-MANAGER-SKILL.md` si la demande concerne cadrage, estimation, planning, capacité, sprint, suivi, priorité, retard ou découpage
- `docs/ai/README-IA.md`
- `docs/ai/WORKFLOW-IA.md`
- `docs/ai/PROJECT-TRACKING.md`
- `docs/ai/CHANGELOG.md`
- `docs/ai/review-checklist.md`
- `docs/ai/REFERENCES.md`
- `docs/standards/CONFIGURATION-STANDARDS.md`
- `docs/standards/FRONTEND-MOBILE-STANDARDS.md`
- `docs/standards/DESIGN-SYSTEM-STANDARDS.md` si la demande concerne Angular, Flutter, UI, thème, design system, composants ou branding
- `DESIGN.md` si le projet contient une interface utilisateur
- `docs/standards/DOCUMENTATION-FIRST.md`
- `docs/release/SEMANTIC-VERSIONING.md` si la demande concerne version, release, tag, changelog, livraison ou breaking change
- `docs/release/RELEASE-WORKFLOW.md` si une version doit être préparée ou publiée
- le ticket concerné dans `docs/ai/tickets` s'il existe

## 2. Interdiction du ticket isolé

Ne jamais traiter une demande comme un ticket isolé.

Pour chaque demande, déterminer d'abord le mode d'intervention :

| Mode | Quand l'utiliser | Fichiers obligatoires |
|---|---|---|
| Diagnostic | Bug, anomalie, incident, comportement incompris | Ticket + rapport diagnostic |
| Engineering | Code, API, DB, UI, Flutter, Angular, Spring | Ticket + checklist + tests |
| Project Manager | Demande macro, estimation, sprint, capacité, retard | Epic/story/task + planning PM |
| QA Review | PR, MR, qualité, régression, merge | Checklist QA + rapport review |
| Architecture | Choix structurant, migration, découpage majeur | ADR + impacts |
| Release | Version, changelog, tag, livraison, hotfix, release candidate | SemVer + changelog + release note |

## 3. Règles générales

Pour chaque intervention :

1. créer ou mettre à jour un fichier dans `docs/ai/tickets` ;
2. écrire les actions à faire ;
3. cocher les actions terminées ;
4. indiquer ce qui reste à faire ;
5. mettre à jour `docs/ai/PROJECT-TRACKING.md` ;
6. mettre à jour `docs/ai/CHANGELOG.md` si le comportement, l'API, la base, l'UI, la configuration ou l'exploitation change ;
7. ajouter ou mettre à jour un ADR si une décision structurante est prise ;
8. mettre à jour les fichiers `docs/pm` si la capacité, le sprint ou le backlog est impacté ;
9. appliquer `docs/release/SEMANTIC-VERSIONING.md` si le changement doit être livré ou versionné ;
10. mettre à jour `VERSION`, `docs/release/VERSION-MATRIX.md` et la release note si une version est préparée.

## 4. Règle macro → découpage

Si la demande est macro, vague ou supérieure à 2-3 jours de travail, ne pas l'implémenter directement.

D'abord produire :

```text
EPIC → User Stories → Tasks → Subtasks
```

Chaque tâche doit avoir :

- un objectif clair ;
- des critères d'acceptation ;
- une estimation ;
- un profil recommandé ;
- une définition de prêt ;
- une définition de fini ;
- un reviewer ;
- des tests attendus.

## 5. Avant toute modification de code

- analyser l'existant ;
- identifier les impacts ;
- vérifier les risques de régression ;
- vérifier la sécurité OWASP ;
- vérifier les principes 12-Factor ;
- vérifier les tests nécessaires ;
- vérifier les impacts Spring Boot, Angular, Flutter, base de données et CI/CD ;
- vérifier les impacts planning : estimation, capacité, sprint, dépendances ;
- vérifier l'impact version : PATCH, MINOR, MAJOR ou aucun bump applicatif.


## 5.1 Standards techniques imposés

## 5.2 Documentation First obligatoire

Avant tout développement significatif, appliquer `docs/standards/DOCUMENTATION-FIRST.md`.

Règle stricte : **la documentation commence au démarrage du développement, pas à la fin**.

Pour chaque epic, user story ou tâche technique, l’IA ou le développeur doit créer ou mettre à jour au minimum :

```text
docs/features/<feature-id>/FUNCTIONAL-SPEC.md
docs/features/<feature-id>/TECHNICAL-DESIGN.md
```

Selon l’impact, ajouter aussi :

```text
docs/features/<feature-id>/API-CONTRACT.md
docs/features/<feature-id>/DATA-MODEL.md
docs/features/<feature-id>/TEST-PLAN.md
docs/features/<feature-id>/USER-GUIDE.md
```

Une tâche n’est pas READY sans documentation initiale. Une tâche n’est pas DONE si la documentation fonctionnelle, technique, API, data, tests, changelog ou release n’est pas à jour.


- **Backend Spring Boot : Maven uniquement.** Utiliser `pom.xml`, `mvnw`, `./mvnw test`, `./mvnw clean verify`. Ne jamais proposer Gradle, `build.gradle`, `settings.gradle` ou `./gradlew` pour un nouveau projet ou une correction, sauf si le dépôt existant l’impose déjà et qu’un ADR documente l’exception.
- **Frontend Angular : Tailwind CSS v4 uniquement.** Ne jamais introduire Angular Material : pas de `@angular/material`, pas de `MatButtonModule`, `MatDialog`, `mat-*`, thème Material ou dépendance Material. Ne jamais introduire Tailwind v3, `@tailwind base/components/utilities` ou `tailwind.config.js` comme source de tokens applicatifs sauf ADR. Les interfaces doivent être conçues avec Tailwind CSS v4, composants Angular maison et design system interne documenté dans `DESIGN.md`.
- **Spring Boot : `application.yml` uniquement.** Utiliser `src/main/resources/application.yml` et les profils YAML (`application-dev.yml`, `application-prod.yml`, `application-test.yml`). Ne jamais créer `application.properties`; si un existant est découvert, le signaler comme dette technique à migrer vers YAML.
- **Angular : `proxy.conf.json` obligatoire.** Tout projet Angular doit contenir un proxy de développement versionné et configuré dans `angular.json` (`proxyConfig`). Les appels HTTP doivent viser des chemins relatifs (`/api`, `/auth`, etc.) et non une URL backend codée en dur.
- Toute violation de ces standards doit être signalée comme risque technique et bloquée en review tant qu’elle n’est pas justifiée par ADR.

## 6. Règles d'exécution

L'IA doit faire des changements minimaux, cohérents avec l'existant.

Elle ne doit jamais :

- supprimer du code sans preuve ;
- changer un contrat API silencieusement ;
- affaiblir la sécurité ;
- retirer des tests pour faire passer le build ;
- ignorer des tests en erreur ;
- inventer une architecture non documentée ;
- créer des tickets immenses non découpés ;
- accepter une tâche sans critères d'acceptation ;
- déclarer une tâche terminée sans preuve de test ou justification.

## 7. Réponse finale obligatoire

La réponse finale doit contenir :

```markdown
## Résumé

## Découpage / Ticket

## Fichiers modifiés

## Tests / vérifications

## Sécurité / Régression

## Impact planning

## Risques restants

## Impact version / SemVer

## Suivi mis à jour

## Reste à faire
```

## Référence configuration obligatoire

Pour toute intervention Spring Boot ou Angular, appliquer aussi :

```text
docs/standards/CONFIGURATION-STANDARDS.md
```


## Standards Frontend/Mobile obligatoires

- **Thème centralisé obligatoire** pour Angular et Flutter : couleurs, typographies, espacements, radius, ombres, breakpoints et états UI doivent être définis dans un design system central, jamais dispersés dans les écrans.
- **Deux thèmes minimum obligatoires** : `light` et `dark`. Le choix du thème doit être piloté par un service/contrôleur central et persistant si le projet le permet.
- **Composants réutilisables obligatoires** : découper les interfaces en composants/widgets partagés (`shared/ui`, `shared/widgets`, design system interne). Éviter les écrans monolithiques.
- **Internationalisation obligatoire** : prévoir au minimum `fr` et `en`. Aucun texte visible utilisateur ne doit être codé en dur dans les composants, templates ou widgets.
- **Configuration applicative centralisée obligatoire** : nom de l’application, logo, baseline, informations éditeur, URLs, paramètres publics, langues supportées et thème par défaut doivent être définis dans un fichier/service de configuration, jamais dupliqués dans les écrans.
- Toute exception doit être documentée dans un ADR accepté.

## Standard DESIGN.md obligatoire

Pour toute demande Angular, Flutter, UI, thème, composant, branding, maquette ou écran :

- lire `DESIGN.md` s’il existe ;
- créer `DESIGN.md` depuis `docs/templates/design/DESIGN.md.example` s’il n’existe pas et que la tâche démarre une UI significative ;
- appliquer `docs/standards/DESIGN-SYSTEM-STANDARDS.md` ;
- Angular doit utiliser Tailwind CSS v4 CSS-first avec `@theme` ;
- Flutter doit mapper les tokens dans le thème central ;
- refuser Tailwind CSS v3, Angular Material, tokens hardcodés et UI sans light/dark/i18n.


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
