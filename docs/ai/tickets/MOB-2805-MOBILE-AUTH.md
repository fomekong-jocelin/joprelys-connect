# MOB-2805 — Auth professionnelle, secure storage, biométrie et frontières de session

## Statut

IN_PROGRESS — issue #254 / PR #255 Draft.

Branche créée depuis `main` au commit `301734a6dc575555c5c1278c4790425c0389ce56`, fusion squash de MOB-2804.

L’implémentation et la documentation détaillée sont présentes. Le gate Flutter complet reste requis avant passage final en review.

## Objectif

Fournir le parcours d’authentification professionnelle de l’application Flutter et brancher la session réelle sur la couche réseau MOB-2804, avec stockage sécurisé, refresh via cookie HttpOnly, biométrie locale et guards de navigation.

## Dépendances

- MOB-2801 : DONE ;
- MOB-2802 : DONE ;
- MOB-2803 : DONE ;
- MOB-2804 : DONE — PR #253 fusionnée dans `main` au commit `301734a6` ;
- backend Joprelys : source de vérité pour login, OTP, refresh, logout et RBAC.

## Contrat backend vérifié

- `POST /api/auth/login` avec `email`, `password` ;
- `POST /api/auth/verify-otp` avec `email`, `otpCode` ;
- `POST /api/auth/refresh` avec cookie HttpOnly professionnel ;
- `POST /api/auth/logout` ;
- réponse : access token, type, expirations, session, identité, rôle et indicateur OTP.

Aucun nouvel endpoint, refresh token lisible ou contrat alternatif n’est introduit.

## Implémentation

- [x] modèle `ProfessionalSession` strict ;
- [x] passerelle `AuthGateway` / `AuthApi` ;
- [x] login professionnel ;
- [x] challenge et vérification OTP ;
- [x] stockage session via `flutter_secure_storage` ;
- [x] stockage cookie via `PersistCookieJar` et adaptateur sécurisé ;
- [x] branchement réel de `ApiSessionAccess` ;
- [x] restauration au démarrage ;
- [x] refresh réel sans récursion d’intercepteur ;
- [x] réutilisation d’un token concurrent plus récent ;
- [x] purge sur refus d’authentification ;
- [x] état de récupération non destructif sur panne réseau ;
- [x] logout local inconditionnel même hors ligne ;
- [x] biométrie locale optionnelle ;
- [x] verrouillage au passage en arrière-plan ;
- [x] guards GoRouter ;
- [x] pages login, OTP, unlock, recovery et loading ;
- [x] FR/EN ;
- [x] light/dark/system ;
- [x] configuration Android et iOS ;
- [x] tests API et cycle de session ;
- [x] documentation fonctionnelle, technique et plan de test ;
- [ ] gate Flutter complet ;
- [ ] changelog et suivi central après preuve runtime ;
- [ ] gate final exact-HEAD.

## Décisions de sécurité

- mot de passe et OTP uniquement en mémoire ;
- access token dans le stockage sécurisé ;
- refresh token exclusivement dans le cookie HttpOnly ;
- cookies persistés par un storage chiffré et namespacé ;
- aucune donnée auth ou clinique dans les logs ;
- réponse malformée rejetée sans compléter les champs ;
- `403` sans refresh/logout ;
- aucune session patient ne peut utiliser le refresh professionnel ;
- biométrie sans émission ni lecture de jeton ;
- logout local prioritaire même si le serveur est indisponible.

## Frontières d’architecture

Aucune modification de :

- `AppTheme` ;
- `AppDesignTokens` ;
- contrat backend ;
- données cliniques ;
- authentification patient.

La composition visuelle est située dans `features/auth/presentation`.

## Tests ajoutés

### `auth_api_test.dart`

- contrat login ;
- challenge OTP ;
- mapping de session complète ;
- réponse refresh malformée ;
- politique logout.

### `auth_session_manager_test.dart`

- purge avant login ;
- absence de session avant succès OTP ;
- restauration directe ;
- refresh expiré ;
- préférence biométrique ;
- refus auth ;
- panne réseau ;
- logout hors ligne ;
- token concurrent plus récent.

## Risques restants

- gate GitHub Actions à obtenir ;
- recette Android réelle avec keystore/device secure storage ;
- recette biométrique sur plusieurs constructeurs Android ;
- validation iOS sur runner/macOS ou appareil iOS ;
- validation backend recette sans déploiement automatique dans ce ticket.

## Critères d’acceptation

- [x] login aligné sur `LoginRequest/LoginResponse` ;
- [x] OTP sans persistance ;
- [x] session sécurisée ;
- [x] CookieJar nettoyé au logout/expiration ;
- [x] restauration tolérante aux pannes réseau ;
- [x] refresh coordonné via MOB-2804 ;
- [x] biométrie fail-safe ;
- [x] routes protégées ;
- [x] séparation patient/professionnel ;
- [x] aucun secret/PII dans les logs et fixtures ;
- [x] FR/EN et thèmes existants ;
- [ ] format, analyse, tests et APK debug verts ;
- [x] documentation de feature mise à jour ;
- [ ] suivi central finalisé.

## Estimation

5 SP — 2 à 3 jours senior / 3 à 4 jours intermédiaire.

## Reviewer

Tech Lead Flutter + backend/auth + sécurité mobile + QA.

## Impact SemVer

Nouvelle capacité interne de l’application mobile avant première diffusion publique. Aucun tag, release ou déploiement dans cette PR.

## Suite

Après validation et fusion :

- MOB-2806 : shell/navigation selon le backlog EPIC-0028 ;
- authentification patient dans son ticket dédié ;
- recette réelle de session/biométrie sur environnement autorisé.
