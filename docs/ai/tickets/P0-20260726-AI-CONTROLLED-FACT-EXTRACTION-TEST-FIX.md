# P0-20260726-AI-CONTROLLED-FACT-EXTRACTION-TEST-FIX

## Qualification

- **Mode** : Diagnostic + Engineering + QA Review
- **Epic** : #192 Engine Anti-hallucination
- **Priorité** : P0 — correction blocage CI PR #196
- **Statut** : DONE — fusionné dans main@df00a904 (PR #196)
- **Stack** : Spring Boot / AI Provider / Conditional Beans
- **Estimation** : 0,25 jour senior
- **Reviewer** : Lead Backend + QA
- **Impact SemVer prévu** : PATCH (correction technique)

## Incident

La CI #1526 de la PR #196 (`feat/ai-controlled-fact-extraction-p0-192`) est rouge avec 351 erreurs de tests suite au chargement échoué du contexte Spring ApplicationContext.

## Diagnostic

`ClinicalFactExtractionService` est annoté `@Service` et dépend de `AiProvider`.
Cependant, `AiProviderConfig` n'enregistre le bean `AiProvider` que sous la condition `@ConditionalOnProperty(name = "joprelys.ai.enabled", havingValue = "true")`.
En l'absence de `@ConditionalOnProperty(name = "joprelys.ai.enabled", havingValue = "true")` sur `ClinicalFactExtractionService`, Spring tente de créer ce bean dans tous les contextes de test d'intégration où l'IA n'est pas activée, provoquant une `UnsatisfiedDependencyException` globale.

## Actions

- [x] Identifier l'absence de `@ConditionalOnProperty` sur `ClinicalFactExtractionService`.
- [x] Ajouter `@ConditionalOnProperty(name = "joprelys.ai.enabled", havingValue = "true")` sur `ClinicalFactExtractionService`.
- [x] Réconcilier la branche `#196` (`feat/ai-controlled-fact-extraction-p0-192`) avec `main@90d5a3c`.
- [ ] Valider l'exécution intégrale des tests Maven (`mvnw clean verify`).
- [ ] Pousser les modifications sur `origin/feat/ai-controlled-fact-extraction-p0-192`.
- [ ] Confirmer que la CI GitHub Actions repasse au vert pour permettre le merge de la PR #196.

## Critères d'acceptation

- [x] `ClinicalFactExtractionService` n'est pas instancié inutilement lorsque `joprelys.ai.enabled` vaut `false` ou n'est pas défini.
- [x] La branche de PR #196 intègre l'ensemble des commits de `main` (notamment #198).
- [ ] Le build backend Maven (`clean verify`) passe à 100% sans erreur.

## Reste à faire

1. Finaliser la vérification Maven.
2. Pousser vers origin.
