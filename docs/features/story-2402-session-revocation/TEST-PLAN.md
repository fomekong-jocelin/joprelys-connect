# STORY-2402 — Plan de test

## Tests domaine/service

- révoquer une session active propre ;
- répéter la révocation sans erreur ;
- refuser une cible d’un autre utilisateur sans permission ;
- autoriser `AUTH_SESSION_MANAGE` dans le même tenant ;
- masquer une cible cross-tenant ;
- logout-all avec zéro, une ou plusieurs familles ;
- état ACTIVE/EXPIRED/REVOKED ;
- access token avec `sid` actif/révoqué/expiré/mauvais utilisateur/mauvais tenant ;
- JWT historique JTI révoqué/non révoqué/expiré.

## Rejeu

- rotation normale ;
- réutilisation de l’ancien refresh ;
- révocation de la génération active de la famille ;
- audit `REFRESH_REPLAY_DETECTED` ;
- second rejeu idempotent sans événement dupliqué ;
- deux rejeux concurrents sans session active résiduelle.

## API MockMvc

- GET sessions courantes ;
- marquage `current` à partir du `sid` ;
- DELETE propre ;
- DELETE administrateur autorisé ;
- DELETE non autorisé ;
- GET administratif same-tenant/cross-tenant ;
- logout courant ;
- logout-all ;
- cookie effacé ;
- erreurs structurées et sans fuite.

## Persistance

- V67 sur H2 ;
- V67 sur PostgreSQL 16 Testcontainers ;
- contraintes et index ;
- redémarrage simulé par lecture dans un nouveau contexte/service ;
- purge des JTI expirés ;
- conservation bornée des audits.

## Non-régression

- login et OTP existants ;
- refresh V66 ;
- JWT patient sans `sid` ;
- RBAC et tenant context ;
- Angular tests/build même sans modification UI.

## Commandes

```bash
cd backend
./mvnw clean verify

cd ../web
npm test
npm run build
```

Aucun test de sécurité ne peut être désactivé ou remplacé par une assertion moins stricte.