# STORY-2402 — Révocation persistante, logout-all et détection du rejeu

## Références

- GitHub : #33
- Epic parent : #29
- Dépendance terminée : #31 / V66
- Branche : `feature/story-2402-session-revocation`
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
7. La révocation de famille et son audit doivent être validés en base même lorsque l’API retourne ensuite `401`.
8. Les opérations de révocation sont idempotentes et ne révèlent jamais l’existence d’une session cross-tenant.

## Action plan

- [x] Vérifier l’absence de branche/PR concurrente.
- [x] Analyser V66, la rotation, le filtre JWT, la blacklist mémoire et le RBAC.
- [x] Définir les frontières personnel/patient.
- [x] Documenter les décisions de révocation et de rejeu.
- [ ] Ajouter la migration V67 additive.
- [ ] Enrichir le domaine de session avec acteur/source de révocation.
- [ ] Ajouter l’audit de sécurité append-only.
- [ ] Remplacer la blacklist mémoire par la persistance JTI bornée.
- [ ] Vérifier en base les access tokens avec `sid`.
- [ ] Ajouter les use cases liste, révocation, logout et logout-all.
- [ ] Ajouter les endpoints et la permission `AUTH_SESSION_MANAGE`.
- [ ] Détecter le rejeu et révoquer toute la famille.
- [ ] Ajouter les tests unitaires, HTTP, concurrence, tenant et PostgreSQL 16.
- [ ] Mettre à jour suivi, changelog et PR.

## Critères d’acceptation

- [ ] Une révocation est effective après redémarrage et sur une autre instance.
- [ ] Un access token moderne cesse immédiatement d’être accepté après révocation de son `sid`.
- [ ] `logout-all` invalide toutes les familles actives du compte.
- [ ] La réutilisation d’un refresh déjà rotaté révoque la famille et crée un audit unique.
- [ ] Un utilisateur ne voit et ne révoque que ses sessions.
- [ ] Un détenteur de `AUTH_SESSION_MANAGE` peut agir uniquement dans son établissement.
- [ ] Une session inexistante et une session cross-tenant produisent le même résultat externe.
- [ ] Aucun secret ni adresse IP complète n’est exposé.
- [ ] Les opérations répétées sont idempotentes.
- [ ] H2 et PostgreSQL 16 sont verts.

## Estimation

- Story points : 5 SP.
- Senior sécurité backend : 2 à 3 jours.
- Intermédiaire encadré : 3,5 à 4,5 jours.
- Reviewer : Tech Lead + référent sécurité.

## Definition of Done

- code, documentation, migration et contrat alignés ;
- chemins autorisés et refusés testés ;
- tests concurrence/rejeu et tenant verts ;
- audit et conservation bornée prouvés ;
- aucun mécanisme mémoire restant ;
- CI complète verte ;
- PR revue avant fusion.