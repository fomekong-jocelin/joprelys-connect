# FUNCTIONAL-SPEC — STORY-1101 : CI/CD Automatisation & Compilation strictes

## Objectif
Automatiser la validation du build et des tests à chaque Pull Request et push sur main/develop.

## Cas d'usage
- Développeur pousse du code → le pipeline se déclenche automatiquement
- Build Maven backend doit compiler sans erreur et exécuter tous les tests
- Build Angular doit compiler et exécuter tous les tests Vitest
- Toute régression bloque le merge

## Critères d'acceptation
- [x] Pipeline CI activé sur branches main, develop et PRs
- [x] Build Maven backend : ./mvnw clean verify -B
- [x] Build Angular : npm ci + npm test + npm run build
- [x] Aucun secret versé dans le dépôt
