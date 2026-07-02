# CLAUDE.md

Lire et respecter obligatoirement :

- `AGENTS.md`
- `SKILL.md`
- `PROJECT-MANAGER-SKILL.md` si la demande concerne projet, sprint, capacité, estimation, retard, priorité ou découpage
- `docs/ai/README-IA.md`
- `docs/ai/WORKFLOW-IA.md`
- `docs/ai/PROJECT-TRACKING.md`
- `docs/ai/CHANGELOG.md`
- `docs/ai/review-checklist.md`
- `docs/ai/REFERENCES.md`
- `docs/standards/DESIGN-SYSTEM-STANDARDS.md` pour toute UI Angular/Flutter
- `DESIGN.md` si le projet a une interface utilisateur
- `docs/release/SEMANTIC-VERSIONING.md` pour toute demande de version/release
- `docs/release/RELEASE-WORKFLOW.md` pour toute livraison

Ne jamais traiter une demande comme un ticket isolé.

Pour chaque intervention :

1. déterminer le mode : Engineering, Project Manager, QA, Diagnostic ou Architecture ;
2. créer ou mettre à jour un ticket dans `docs/ai/tickets` ;
3. découper en epic/story/task si la demande est macro ;
4. estimer selon `docs/pm/ESTIMATION-GUIDE.md` ;
5. vérifier la capacité selon `docs/pm/CAPACITY-PLANNING.md` ;
6. cocher ce qui est terminé ;
7. documenter ce qui reste ;
8. mettre à jour le suivi projet ;
9. mettre à jour le changelog si nécessaire ;
10. appliquer SemVer et proposer le bon bump si une livraison est concernée.


## Standards techniques obligatoires

- Backend Spring Boot : utiliser Maven uniquement (`pom.xml`, `mvnw`, `./mvnw test`, `./mvnw clean verify`). Ne pas proposer Gradle sauf exception existante documentée par ADR.
- Frontend Angular : utiliser Tailwind CSS v4 uniquement. Angular Material est interdit par défaut (`@angular/material`, `Mat*`, `mat-*`, thèmes Material), sauf ADR exceptionnelle validée.
- Spring Boot : utiliser `application.yml` et les profils YAML, jamais `application.properties` sauf dette existante documentée par ADR.
- Angular : vérifier la présence de `proxy.conf.json` et son référencement dans `angular.json`; les appels API doivent rester relatifs.

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

## Documentation First

Avant tout développement, appliquer `docs/standards/DOCUMENTATION-FIRST.md`.

Créer ou mettre à jour la documentation fonctionnelle et technique dès le démarrage :

```text
docs/features/<feature-id>/FUNCTIONAL-SPEC.md
docs/features/<feature-id>/TECHNICAL-DESIGN.md
```

Ne pas considérer une tâche comme terminée si la documentation impactée n’est pas à jour.

## UI / Design System obligatoire

Pour toute UI Angular ou Flutter : lire ou créer `DESIGN.md`, appliquer `docs/standards/DESIGN-SYSTEM-STANDARDS.md`, utiliser Tailwind CSS v4 côté Angular, refuser Tailwind v3, `tailwind.config.js` comme source de tokens applicatifs et Angular Material sauf ADR.


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
