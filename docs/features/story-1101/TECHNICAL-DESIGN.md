# TECHNICAL-DESIGN — STORY-1101

## Architecture CI
- Platform : GitHub Actions
- Fichier : .github/workflows/ci.yml
- Jobs : backend-build (Java 21 + Maven) + frontend-build (Node 22 + Angular)
- Cache Maven : ~/.m2 via actions/setup-java
- Cache npm : node_modules via actions/setup-node

## Sécurité
- JWT secret en variable d'environnement CI (valeur de test uniquement)
- Aucun secret de production dans le workflow

## Dépendances
- GitHub Actions marketplace : actions/checkout@v4, setup-java@v4, setup-node@v4
