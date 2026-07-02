# GEMINI.md

Tu dois appliquer les règles suivantes avant toute action.

@./AGENTS.md
@./SKILL.md
@./PROJECT-MANAGER-SKILL.md
@./docs/ai/README-IA.md
@./docs/ai/WORKFLOW-IA.md
@./docs/ai/PROJECT-TRACKING.md
@./docs/ai/CHANGELOG.md
@./docs/ai/review-checklist.md
@./docs/ai/REFERENCES.md
@./docs/pm/SCRUM-WORKFLOW.md
@./docs/pm/CAPACITY-PLANNING.md
@./docs/pm/ESTIMATION-GUIDE.md
@./docs/pm/TEAM-PROFILES.md
@./docs/release/SEMANTIC-VERSIONING.md
@./docs/release/RELEASE-WORKFLOW.md

Règles obligatoires :

- Ne jamais travailler comme un simple exécutant de ticket.
- Activer `PROJECT-MANAGER-SKILL.md` pour toute demande macro, sprint, estimation, capacité, retard, priorité ou suivi.
- Créer ou mettre à jour un ticket dans `docs/ai/tickets/`.
- Découper toute demande macro en epic, stories, tasks et subtasks.
- Estimer par complexité, risque, profil recommandé et effort.
- Cocher les actions terminées.
- Mettre à jour `PROJECT-TRACKING.md`.
- Mettre à jour `CHANGELOG.md` si nécessaire.
- Appliquer Semantic Versioning pour toute livraison, release, tag, hotfix ou breaking change.


## Standards techniques obligatoires

- Backend Spring Boot : utiliser Maven uniquement (`pom.xml`, `mvnw`, `./mvnw test`, `./mvnw clean verify`). Ne pas proposer Gradle sauf exception existante documentée par ADR.
- Frontend Angular : utiliser Tailwind CSS uniquement. Angular Material est interdit par défaut (`@angular/material`, `Mat*`, `mat-*`, thèmes Material), sauf ADR exceptionnelle validée.
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
