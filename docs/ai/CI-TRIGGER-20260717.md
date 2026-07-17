# Déclenchement CI — 2026-07-17

Ce commit déclenche la pipeline complète sur `main` après ajout des tests ciblés de l’intégration OpenAI.

Contrôles attendus :

- backend : `./mvnw clean verify -B -Dspring.profiles.active=test` ;
- frontend : `npm test` ;
- frontend : `npm run build`.
