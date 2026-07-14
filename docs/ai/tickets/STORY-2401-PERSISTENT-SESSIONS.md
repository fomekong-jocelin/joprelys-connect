# STORY-2401 — Sessions persistantes et rotation des refresh tokens

## Références

- GitHub : #31
- Epic parent : #29
- Branche : `feature/story-2401-persistent-sessions`
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

1. **Cookie web HttpOnly** : le refresh token brut ne sera jamais présent dans le JSON, le DOM, `localStorage` ou `sessionStorage`.
2. **Token opaque aléatoire** : aucune donnée utilisateur n’est encodée dans le refresh token.
3. **Hash SHA-256 en base** : le token brut n’est pas persistant ; la forte entropie du token rend le hash adapté à la recherche exacte.
4. **Une ligne par rotation** : l’ancienne ligne reste révoquée et pointe vers sa remplaçante. Cela prépare la détection de rejeu de #33 sans dupliquer le modèle.
5. **Verrou pessimiste lors du refresh** : deux requêtes concurrentes avec le même token ne peuvent pas réussir toutes les deux.
6. **Migration additive V66** : aucune table ou colonne existante n’est détruite.
7. **Compatibilité JWT** : le claim `sid` est ajouté aux nouveaux tokens ; les tokens historiques sans `sid` restent parseables pendant la transition.

## Action plan

- [x] Vérifier les branches et PR existantes.
- [x] Analyser `AuthenticationService`, `AuthController`, `JwtService`, la blacklist et Angular.
- [x] Définir le périmètre personnel/patient.
- [x] Justifier le stockage du refresh token par ADR.
- [ ] Ajouter la migration V66 et ses index/contraintes.
- [ ] Ajouter les propriétés de session en YAML.
- [ ] Implémenter génération, hash et modèle persistant.
- [ ] Implémenter l’émission de session au login et après OTP.
- [ ] Ajouter `POST /api/auth/refresh`.
- [ ] Implémenter la rotation atomique et la purge.
- [ ] Ajouter les tests unitaires, API, intégration et concurrence.
- [ ] Valider H2 et PostgreSQL 16.
- [ ] Mettre à jour le suivi, le changelog et la PR.

## Critères d’acceptation

- [ ] Le login personnel émet un access token et un cookie de refresh sécurisé.
- [ ] Aucun refresh token brut n’est stocké ni journalisé.
- [ ] Un refresh produit un nouvel access token, un nouveau cookie et révoque l’ancien token.
- [ ] Deux refresh concurrents avec le même token produisent exactement un succès.
- [ ] Une session active reste renouvelable après reconstruction du service depuis la base.
- [ ] L’expiration absolue et l’inactivité sont appliquées côté backend.
- [ ] Un utilisateur désactivé ou appartenant à une organisation inactive ne peut pas renouveler.
- [ ] Les tests H2 et PostgreSQL 16 sont verts.

## Estimation

- Story points : 8 SP — sécurité, concurrence, API et migration.
- Senior sécurité backend : 2,5 à 3,5 jours.
- Intermédiaire encadré : 4 à 5 jours.
- Reviewer : Tech Lead + référent sécurité.

## Risques

- changement du contrat interne de login ;
- concurrence sur la rotation ;
- cookie non envoyé en local si `Secure` est activé hors HTTPS ;
- compatibilité temporaire des JWT sans `sid` ;
- dette OTP en mémoire maintenue hors de cette story.

## Definition of Done

- code et documentation alignés ;
- migrations H2/PostgreSQL validées ;
- chemins sécurité couverts ;
- aucun secret exposé ;
- CI complète verte ;
- PR revue avant fusion ;
- #31 clôturée uniquement après preuves.