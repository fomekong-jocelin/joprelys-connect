# Changelog — STORY-2401 Sessions persistantes et rotation

- Date : 2026-07-14
- Ticket : #31
- Epic : #29
- PR : #54
- Impact SemVer : MINOR

## Ajouté

- table `auth_sessions` et migration additive V66 ;
- familles de sessions et générations successives de refresh tokens ;
- refresh token opaque de 256 bits ;
- hash SHA-256 persistant, sans stockage du secret brut ;
- endpoint `POST /api/auth/refresh` ;
- rotation atomique sous verrou pessimiste ;
- expirations absolue et d’inactivité ;
- purge planifiée des sessions expirées ;
- cookie web `HttpOnly`, `SameSite=Lax`, limité à `/api/auth` et `Secure` en production ;
- claim JWT optionnel `sid` ;
- profil `application-prod.yml` exigeant les secrets externes ;
- documentation fonctionnelle, technique, API, données, tests et ADR.

## Modifié

- le login personnel et la validation OTP créent une session persistante après authentification complète ;
- `LoginResponse` expose de façon additive `sessionId` et `sessionExpiresAt` ;
- le logout historique efface également le cookie de refresh ;
- les erreurs de refresh utilisent le code stable `AUTH_SESSION_INVALID` ;
- l’OTP du personnel n’est plus écrit dans la console ;
- les nouveaux access tokens du personnel portent leur identifiant de session ;
- les anciens JWT et les JWT patients restent compatibles sans `sid`.

## Sécurité

- aucun refresh token brut dans le JSON, la base ou les logs du nouveau flux ;
- les secrets PostgreSQL, JWT et laboratoire sont obligatoires dans le profil production ;
- OTP, seed admin et Swagger sont désactivés en production ;
- métadonnées client bornées : user-agent normalisé, adresse réseau masquée ;
- un refresh token ne peut réussir qu’une fois ;
- une organisation inactive ou un compte désactivé ne peut pas renouveler.

## Validation

- GitHub Actions run #625 : vert ;
- Maven `clean verify` : vert ;
- migrations H2 : vertes ;
- PostgreSQL 16 via Testcontainers : vert ;
- concurrence sur un même refresh token : un seul succès ;
- tests Angular et build production : verts ;
- aucun test désactivé ou contourné.

## Hors périmètre conservé

- révocation persistante, rejeu de famille et `logout-all` : #33 ;
- intercepteur Angular, renouvellement single-flight et écran des sessions : #34 ;
- authentification patient persistante ;
- fournisseur OTP externe, OIDC et passkeys.

## Rollback

- désactiver l’usage de `/api/auth/refresh` côté client ;
- conserver la table V66, car la migration est additive et non destructive ;
- les access tokens historiques restent utilisables jusqu’à leur expiration ;
- ne jamais supprimer V66 d’une base où elle a été appliquée.