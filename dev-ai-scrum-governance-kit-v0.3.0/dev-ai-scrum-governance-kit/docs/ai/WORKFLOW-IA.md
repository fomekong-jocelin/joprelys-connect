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

- [ ] `./mvnw test` ou `./gradlew test`
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
