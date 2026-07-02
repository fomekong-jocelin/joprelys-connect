# WORKFLOW IA — Procédure obligatoire

## 1. Démarrage

Avant toute action :

- [ ] Lire `AGENTS.md`
- [ ] Lire `SKILL.md`
- [ ] Lire `README-IA.md`
- [ ] Lire `PROJECT-TRACKING.md`
- [ ] Lire `CHANGELOG.md`
- [ ] Lire `review-checklist.md`
- [ ] Identifier ou créer le ticket
- [ ] Activer `PROJECT-MANAGER-SKILL.md` si la demande est macro, planning, estimation, sprint ou capacité

## 2. Qualification de la demande

Classer la demande :

| Type | Action |
|---|---|
| Bug | Diagnostic + correction + tests |
| Feature | Story/task + implémentation + tests |
| Refactoring | Risques + tests de non-régression |
| Security | Analyse sécurité + tests dédiés |
| Architecture | ADR obligatoire |
| Macro besoin | Découpage PM obligatoire |
| Planning | Capacity planning obligatoire |
| Review | Checklist QA obligatoire |

## 3. Compréhension

Documenter :

- besoin métier ;
- résultat attendu ;
- critères d'acceptation ;
- modules impactés ;
- hypothèses ;
- risques ;
- dépendances ;
- profil recommandé ;
- estimation initiale.

## 4. Analyse de l'existant

Inspecter avant modification :

- code existant ;
- architecture ;
- tests ;
- configuration ;
- API contracts ;
- base de données et migrations ;
- flows UI Angular/Flutter ;
- sécurité ;
- logs et monitoring ;
- CI/CD.

## 5. Plan d'action

Créer une checklist dans le ticket :

```markdown
## Action plan
- [ ] Comprendre le comportement actuel
- [ ] Identifier les fichiers impactés
- [ ] Vérifier la capacité et le découpage si tâche macro
- [ ] Modifier l'implémentation
- [ ] Ajouter ou adapter les tests
- [ ] Exécuter les vérifications
- [ ] Mettre à jour la documentation
- [ ] Mettre à jour le changelog
- [ ] Mettre à jour le suivi global
```

## 6. Implémentation

Règles :

- changements minimaux ;
- cohérence avec l'existant ;
- séparation des responsabilités ;
- aucun contournement rapide ;
- pas de refactoring massif sans justification ;
- pas de suppression sans preuve ;
- pas de changement de contrat silencieux.

## 7. Vérifications

### Backend Spring Boot

- [ ] `./mvnw test`
- [ ] `./mvnw clean verify` si build complet ou CI impacté
- [ ] Vérifier que le backend utilise Maven uniquement (`pom.xml`, `mvnw`) et qu’aucune configuration Gradle n’est introduite
- [ ] Vérifier que la configuration Spring Boot est en YAML : `application.yml` / `application-<profile>.yml`, sans nouveau `application.properties`
- [ ] tests unitaires domaine/service
- [ ] tests API/controller
- [ ] tests sécurité AuthN/AuthZ
- [ ] tests repository/migration si base de données

### Angular

- [ ] `npm run lint`
- [ ] `npm run test`
- [ ] `npm run build`
- [ ] tests composants/services/guards/interceptors
- [ ] vérification sécurité XSS/CSP si contenu HTML
- [ ] Vérifier que l’UI Angular utilise Tailwind CSS v4 CSS-first
- [ ] Vérifier qu’aucune dépendance, module ou composant Angular Material n’est introduit
- [ ] Vérifier la présence de `proxy.conf.json` et son référencement dans `angular.json` via `proxyConfig`
- [ ] Vérifier que les services Angular utilisent des chemins API relatifs et aucune URL backend hardcodée

### Flutter

- [ ] `flutter analyze`
- [ ] `flutter test`
- [ ] tests providers/use cases/widgets
- [ ] vérification secure storage si données sensibles

### CI/CD

- [ ] pipeline lint/build/test
- [ ] scans sécurité
- [ ] migrations testées
- [ ] rollback documenté si changement risqué

## 8. Mise à jour documentaire

À la fin :

- [ ] Mettre à jour le ticket
- [ ] Mettre à jour `PROJECT-TRACKING.md`
- [ ] Mettre à jour `CHANGELOG.md`
- [ ] Ajouter un ADR si décision structurante
- [ ] Mettre à jour `docs/pm/` si impact planning

## 9. Réponse finale attendue

```markdown
## Résumé

## Découpage / Ticket

## Fichiers modifiés

## Tests / vérifications

## Sécurité

## Impact planning

## Risques restants

## Suivi mis à jour

## Reste à faire
```


## 9. Versioning et release

Si l'intervention produit un changement livrable :

- [ ] Lire `docs/release/SEMANTIC-VERSIONING.md`
- [ ] Déterminer l'impact SemVer : aucun / PATCH / MINOR / MAJOR
- [ ] Identifier les breaking changes
- [ ] Vérifier si `VERSION` doit être mis à jour
- [ ] Vérifier si `docs/release/VERSION-MATRIX.md` doit être mis à jour
- [ ] Mettre à jour `docs/ai/CHANGELOG.md`
- [ ] Préparer une release note si une livraison est demandée

La réponse finale doit préciser :

```markdown
## Impact version / SemVer
Version actuelle :
Version proposée :
Bump : Aucun / PATCH / MINOR / MAJOR
Justification :
Breaking changes : Oui / Non
```

## Standards UI obligatoires à vérifier

Pour chaque tâche Angular ou Flutter, vérifier avant développement et avant clôture :

- thème centralisé existant ou créé ;
- thèmes `light` et `dark` couverts ;
- composants/widgets réutilisables utilisés ;
- textes visibles internationalisés au minimum en `fr` et `en` ;
- nom de l’app, logo, slogan, liens publics et paramètres d’affichage centralisés dans la configuration ;
- aucune valeur de branding ou chaîne utilisateur hardcodée dans les écrans.

## Workflow UI DESIGN.md

Pour toute tâche Angular/Flutter UI :

- [ ] Lire `DESIGN.md` ou créer une première version depuis `docs/templates/design/DESIGN.md.example`.
- [ ] Lire `docs/standards/DESIGN-SYSTEM-STANDARDS.md`.
- [ ] Vérifier les tokens : couleurs, typographie, spacing, radius, composants.
- [ ] Vérifier l’approche Angular Tailwind CSS v4 (`@import "tailwindcss"`, `@theme`).
- [ ] Vérifier qu’aucun Tailwind v3 / `@tailwind base/components/utilities` / `tailwind.config.js` applicatif n’est introduit.
- [ ] Vérifier qu’aucun Angular Material n’est introduit sans ADR.
- [ ] Vérifier le mapping Flutter ThemeData/tokens si mobile.
- [ ] Vérifier light/dark, i18n FR/EN, composants partagés et accessibilité.

## Vérification UI — Arrondis et ombres

- [ ] `docs/standards/UI-RADIUS-AND-SHADOW-STANDARDS.md` lu si UI Angular/Flutter.
- [ ] Les cards, formulaires, inputs et boutons utilisent des coins carrés ou légèrement arrondis.
- [ ] Aucun usage injustifié de `rounded-full`, `rounded-2xl`, `rounded-3xl` ou équivalent.
- [ ] Les rayons sont centralisés dans les tokens design/Tailwind v4/Flutter theme.
- [ ] Les ombres sont sobres et cohérentes light/dark.
- [ ] Toute exception est documentée dans `DESIGN.md` ou ADR.


## Vérification architecture obligatoire — SOLID et responsabilités

Avant toute implémentation :

- [ ] Lire `docs/standards/ARCHITECTURE-SOLID-RESPONSIBILITY-STANDARDS.md`.
- [ ] Identifier la couche responsable : controller, service/use case, domain, infrastructure, composant UI ou widget.
- [ ] Vérifier que le backend porte la règle métier et que le front ne fait que l’affichage/orchestration UX.
- [ ] Vérifier qu’aucune logique métier n’est prévue dans un controller.
- [ ] Vérifier qu’aucun composant Angular/Flutter ne devient monolithique.
- [ ] Vérifier les interfaces, implémentations, abstractions, généricité et héritage nécessaires.
- [ ] Vérifier les limites de taille : 500 lignes maximum, alerte dès 300 lignes.
- [ ] Prévoir les tests backend des règles métier et les tests UI des états d’affichage.


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
