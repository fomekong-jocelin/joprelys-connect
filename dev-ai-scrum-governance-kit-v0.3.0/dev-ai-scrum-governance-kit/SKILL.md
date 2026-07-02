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
8. le ticket concerné dans `docs/ai/tickets/`

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

- Respecter le flux `Controller → Application/Service → Domain ← Infrastructure`.
- Ne jamais exposer les entités JPA en réponse API.
- DTO immuables.
- Transactions au niveau service, pas controller.
- Validation aux frontières.
- Erreurs structurées, pas de stack trace exposée.
- Requêtes paramétrées.
- Tests unitaires, slice tests, intégration selon impact.

### Angular

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
./gradlew test

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
