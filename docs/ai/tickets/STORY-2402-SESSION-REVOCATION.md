# STORY-2402 — Révocation persistante, logout-all et détection du rejeu

## Références

- GitHub : #33
- Epic parent : #29
- Dépendance terminée : #31 / V66
- Branche : `feature/story-2402-session-revocation`
- PR : #55
- Mode : Engineering + Security + Architecture + Documentation First

## Objectif

Permettre au personnel et aux administrateurs habilités de révoquer réellement des sessions sur toutes les instances, fermer toutes les sessions d’un compte et détecter la réutilisation d’un refresh token déjà consommé.

## Périmètre confirmé

- sessions du personnel uniquement ;
- liste des sessions du compte courant ;
- liste administrative d’un collaborateur du même établissement ;
- révocation d’une session propre ou d’une session du même tenant avec permission ;
- logout de la session courante ;
- logout-all du compte courant ;
- rejeu d’un refresh déjà rotaté ;
- validation persistante des access tokens liés à un `sid` ;
- remplacement de la blacklist mémoire des JTI historiques ;
- audit append-only des événements de session.

## Hors périmètre justifié

- refresh automatique Angular et écran des sessions : #34 ;
- sessions patient : contrat distinct sans `user_id` ;
- révocation distante inter-tenant par l’administration plateforme ;
- OIDC, passkeys, WebAuthn et fournisseur OTP externe.

## Décisions

1. `auth_sessions` reste l’unique source de vérité des sessions modernes.
2. Les JWT avec `sid` sont refusés dès que la session est révoquée, expirée ou ne correspond plus au compte/tenant.
3. Une table `revoked_access_tokens` remplace la blacklist mémoire uniquement pour les JWT historiques sans `sid`.
4. Une permission dédiée `AUTH_SESSION_MANAGE` autorise l’administration same-tenant ; les utilisateurs gèrent toujours leurs propres sessions.
5. Un audit `auth_session_audit_events` conserve création, rotation, révocation, logout-all et rejeu.
6. Le rejeu est détecté uniquement si le token présenté correspond à une génération marquée `ROTATED`.
7. La révocation de famille et son audit sont validés en base même lorsque l’API retourne ensuite `401`.
8. Les opérations de révocation sont idempotentes et ne révèlent jamais l’existence d’une session cross-tenant.
9. Les contraintes de vocabulaire V67 utilisent `CASE ... THEN TRUE` afin de rester strictes et portables sur H2/PostgreSQL 16.
10. La durée d’audit reste configurable ; la valeur production par défaut de 90 jours doit être validée par le DPO avant mise en production.

## Action plan

- [x] Vérifier l’absence de branche/PR concurrente.
- [x] Analyser V66, la rotation, le filtre JWT, la blacklist mémoire et le RBAC.
- [x] Définir les frontières personnel/patient.
- [x] Documenter les décisions de révocation et de rejeu.
- [x] Ajouter la migration V67 additive.
- [x] Enrichir le domaine de session avec acteur/source de révocation.
- [x] Ajouter l’audit de sécurité append-only.
- [x] Remplacer la blacklist mémoire par la persistance JTI bornée.
- [x] Vérifier en base les access tokens avec `sid`.
- [x] Ajouter les use cases liste, révocation, logout et logout-all.
- [x] Ajouter les endpoints et la permission `AUTH_SESSION_MANAGE`.
- [x] Détecter le rejeu et révoquer toute la famille.
- [x] Ajouter les tests unitaires, HTTP, concurrence, tenant et PostgreSQL 16.
- [x] Corriger le test après extraction de `AuthSessionViewMapper`.
- [x] Stabiliser les contraintes V67 sur H2/PostgreSQL 16.
- [x] Mettre à jour suivi, changelog et PR.
- [x] Follow-up sécurité 2026-07-18 : ajouter la purge navigateur complète, `Clear-Site-Data` et la neutralisation du cookie professionnel lors d'un login patient.

## Critères d’acceptation

- [x] Une révocation est effective après redémarrage et sur une autre instance via la base partagée.
- [x] Un access token moderne cesse immédiatement d’être accepté après révocation de son `sid`.
- [x] `logout-all` invalide toutes les familles actives du compte.
- [x] La réutilisation d’un refresh déjà rotaté révoque la famille et crée un audit unique.
- [x] Un utilisateur ne voit et ne révoque que ses sessions.
- [x] Un détenteur de `AUTH_SESSION_MANAGE` peut agir uniquement dans son établissement.
- [x] Une session inexistante et une session cross-tenant produisent le même résultat externe.
- [x] Aucun secret ni adresse IP complète n’est exposé.
- [x] Les opérations répétées sont idempotentes.
- [x] H2 et PostgreSQL 16 sont verts.

## Preuves de validation

GitHub Actions #643, commit `41875195` :

- Maven `clean verify` : succès ;
- migrations H2 : succès ;
- PostgreSQL 16 / Testcontainers : succès ;
- tests de gestion des sessions et RBAC : succès ;
- rejeu et concurrence : succès ;
- tests Angular : succès ;
- build Angular production : succès.

## Estimation

- Story points : 5 SP.
- Senior sécurité backend : 2 à 3 jours.
- Intermédiaire encadré : 3,5 à 4,5 jours.
- Reviewer : Tech Lead + référent sécurité.

## Definition of Done

- [x] code, documentation, migration et contrat alignés ;
- [x] chemins autorisés et refusés testés ;
- [x] tests concurrence/rejeu et tenant verts ;
- [x] audit et conservation bornée prouvés ;
- [x] aucun mécanisme mémoire restant pour la révocation ;
- [x] CI complète verte ;
- [ ] revue humaine avant fusion.
