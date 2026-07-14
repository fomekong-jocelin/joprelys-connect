# STORY-2401 — Sessions persistantes et rotation des refresh tokens

## Références

- GitHub : #31
- Epic parent : #29
- Branche : `feature/story-2401-persistent-sessions`
- PR : #54
- Mode : Engineering + Security + Architecture + Documentation First

## Objectif

Permettre aux comptes du personnel de conserver une session renouvelable après redémarrage du backend, avec un access token court et un refresh token opaque, rotatif et stocké uniquement sous forme de hash.

## Périmètre confirmé

- authentification du personnel exposée sous `/api/auth` ;
- persistance des familles de sessions ;
- émission d’un cookie de refresh web `HttpOnly` ;
- rotation atomique ;
- expiration absolue et expiration d’inactivité ;
- purge planifiée des sessions expirées ;
- compatibilité additive avec le contrat de login actuel ;
- H2 et PostgreSQL 16.

## Hors périmètre justifié

- révocation d’une famille compromise, `logout-all` et catalogue des sessions : #33 ;
- intercepteur Angular, refresh single-flight et écran des sessions : #34 ;
- OTP patient : flux séparé sans `user_id`, non couvert par le modèle de #31 ;
- fournisseur SMS/e-mail, OIDC, passkeys et WebAuthn : hors périmètre de l’EPIC #29.

## Décisions

1. **Cookie web HttpOnly** : le refresh token brut n’est jamais présent dans le JSON, le DOM, `localStorage` ou `sessionStorage`.
2. **Token opaque aléatoire** : aucune donnée utilisateur n’est encodée dans le refresh token.
3. **Hash SHA-256 en base** : le token brut n’est pas persistant ; la forte entropie du token rend le hash adapté à la recherche exacte.
4. **Une ligne par rotation** : l’ancienne ligne reste révoquée et pointe vers sa remplaçante. Cela prépare la détection de rejeu de #33 sans dupliquer le modèle.
5. **Verrou pessimiste lors du refresh** : deux requêtes concurrentes avec le même token ne peuvent pas réussir toutes les deux.
6. **Migration additive V66** : aucune table ou colonne existante n’est détruite.
7. **Compatibilité JWT** : le claim `sid` est ajouté aux nouveaux tokens ; les tokens historiques sans `sid` restent parseables pendant la transition.
8. **Profil production fail-fast** : les secrets et identifiants sensibles sont obligatoirement injectés par l’environnement ; OTP, seed admin et Swagger sont désactivés.
9. **Internationalisation différée sans dette de texte** : le backend retourne le code stable `AUTH_SESSION_INVALID`; #34 portera les libellés FR/EN dans Angular.

## Action plan

- [x] Vérifier les branches et PR existantes.
- [x] Analyser `AuthenticationService`, `AuthController`, `JwtService`, la blacklist et Angular.
- [x] Définir le périmètre personnel/patient.
- [x] Justifier le stockage du refresh token par ADR.
- [x] Ajouter la migration V66 et ses index/contraintes.
- [x] Ajouter les propriétés de session en YAML et un profil production sécurisé.
- [x] Implémenter génération, hash et modèle persistant.
- [x] Implémenter l’émission de session au login et après OTP.
- [x] Ajouter `POST /api/auth/refresh`.
- [x] Implémenter la rotation atomique et la purge.
- [x] Ajouter les tests unitaires, API, intégration et concurrence.
- [x] Valider H2 et PostgreSQL 16.
- [x] Mettre à jour le suivi, le changelog et la PR.

## Critères d’acceptation

- [x] Le login personnel émet un access token et un cookie de refresh sécurisé.
- [x] Aucun refresh token brut n’est stocké ni retourné dans le JSON ou journalisé par le nouveau flux.
- [x] Un refresh produit un nouvel access token, un nouveau cookie et révoque l’ancien token.
- [x] Deux refresh concurrents avec le même token produisent exactement un succès.
- [x] Une session active est renouvelée depuis l’état persistant en base, sans map mémoire.
- [x] L’expiration absolue et l’inactivité sont appliquées côté backend.
- [x] Un utilisateur désactivé ou appartenant à une organisation inactive ne peut pas renouveler.
- [x] Les tests H2 et PostgreSQL 16 sont verts.

## Preuves de validation

- GitHub Actions : run #625 sur le commit `57bf9261` ;
- backend Maven strict : réussi ;
- frontend Angular tests et build production : réussis ;
- migration V66 validée sur H2 et PostgreSQL 16 ;
- test HTTP login → cookie → refresh → ancien token refusé : réussi ;
- test de concurrence : exactement une rotation réussie ;
- test de génération 256 bits et hash SHA-256 : réussi ;
- profil `prod` exige les secrets externes et force `Secure=true`.

## Estimation

- Story points : 8 SP — sécurité, concurrence, API et migration.
- Senior sécurité backend : 2,5 à 3,5 jours.
- Intermédiaire encadré : 4 à 5 jours.
- Reviewer : Tech Lead + référent sécurité.

## Risques résiduels

- la révocation persistante de logout et la détection de rejeu de famille sont volontairement dans #33 ;
- le renouvellement automatique et l’écran des sessions sont volontairement dans #34 ;
- les JWT historiques sans `sid` restent acceptés jusqu’à leur expiration ;
- le mécanisme OTP du personnel reste en mémoire jusqu’au chantier OTP final, mais il n’est plus écrit en console et n’est pas exposé en profil `prod`.

## Definition of Done

- [x] code et documentation alignés ;
- [x] migrations H2/PostgreSQL validées ;
- [x] chemins sécurité couverts ;
- [x] aucun refresh token brut exposé ;
- [x] CI complète verte ;
- [ ] revue humaine avant fusion ;
- [ ] clôture automatique de #31 lors de la fusion.