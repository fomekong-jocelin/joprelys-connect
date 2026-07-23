# HOS-RBAC-001-D — Suppression définitive de `HOSPITALIZATION_MANAGE`

## Métadonnées

- **Epic** : EPIC-0027 — Organisation hospitalière, capacité et parcours patient
- **Story** : HOS-RBAC-001 — Autorisations hospitalières contextuelles
- **Issue GitHub** : #121
- **Dépendance livrée** : HOS-RBAC-001-C / PR #107
- **Priorité** : P0 sécurité / phase de développement
- **Type** : refactoring sécurité + migration DB de nettoyage
- **Profil recommandé** : Senior backend sécurité + DBA/reviewer sécurité
- **Estimation** : 2 SP / 0,5 à 1 j senior
- **Reviewer** : Tech Lead + RSSI/DBA
- **Statut** : IN_PROGRESS

## Contexte

HOS-RBAC-001-C a remplacé les écritures hospitalières génériques par des permissions orientées intention. `HOSPITALIZATION_MANAGE` n'est plus utilisé par les endpoints ni distribué aux rôles système concernés, mais reste encore catalogué uniquement pour faciliter une rétro-compatibilité des rôles personnalisés.

Le produit est encore en phase de développement. Cette compatibilité temporaire n'est pas retenue : conserver une permission large, sans usage métier courant, crée une dette de sécurité et une source de régression future.

## Décision

Supprimer complètement `HOSPITALIZATION_MANAGE` :

1. du catalogue Java ;
2. du référentiel persistant via Flyway V86 ;
3. des associations de rôles personnalisés existantes par suppression en cascade ;
4. des documents qui la présentent encore comme permission actuelle ou temporairement conservée.

La suppression est **fail closed** : aucun mapping automatique vers les nouvelles permissions n'est autorisé. Un rôle personnalisé doit recevoir explicitement les permissions métier nécessaires.

## Périmètre

### Inclus

- `RbacCatalog` ;
- Flyway V86 ;
- tests catalogue/migration ;
- API contract et documentation HOS-RBAC ;
- matrice d'audit ;
- backlog EPIC-0027 ;
- `PROJECT-TRACKING.md` ;
- `CHANGELOG.md`.

### Exclus

- ABAC unité/relation de soin ;
- habilitations professionnelles et délégations ;
- clearance de sortie ;
- refonte générale du mécanisme `user.role` legacy ;
- modification des migrations V1–V85 déjà appliquées.

## Critères d'acceptation

- [ ] `RbacCatalog.permissionCodes()` ne contient plus `HOSPITALIZATION_MANAGE`.
- [ ] Aucun rôle système ne contient cette permission.
- [ ] Aucun endpoint backend n'utilise cette permission.
- [ ] Aucun écran Angular n'utilise cette permission comme fallback.
- [ ] V86 supprime `HOSPITALIZATION_MANAGE` de `permissions`.
- [ ] Les liens `role_permissions` associés disparaissent via la FK `ON DELETE CASCADE`.
- [ ] Aucun mapping automatique vers les permissions HOS-RBAC-001-B/C n'est effectué.
- [ ] Un test empêche la réintroduction de la permission dans le catalogue.
- [ ] Un test de migration valide un upgrade V85 → V86 avec rôle personnalisé lié à la permission legacy.
- [ ] Le greenfield Flyway V1 → V86 reste valide.
- [ ] La suite Maven complète est verte.
- [ ] La suite Angular concernée est verte.
- [ ] Toutes les documentations décrivant l'état courant sont alignées.

## Action plan

- [x] Rechercher toutes les références `HOSPITALIZATION_MANAGE` dans le dépôt.
- [x] Vérifier qu'aucun endpoint ou composant actif ne dépend encore de cette permission.
- [x] Vérifier la FK `role_permissions.permission_code -> permissions.code ON DELETE CASCADE`.
- [ ] Retirer la permission du catalogue Java.
- [ ] Ajouter Flyway V86 de suppression fail-closed.
- [ ] Ajouter/adapter les tests catalogue et migration.
- [ ] Aligner API contract, audit, backlog, HOS-RBAC-001-C et tests documentaires.
- [ ] Mettre à jour `PROJECT-TRACKING.md` et `CHANGELOG.md`.
- [ ] Exécuter les tests ciblés puis `./mvnw clean verify`.
- [ ] Exécuter les tests Angular concernés et le build si la CI de détection l'exige.
- [ ] Créer une PR Ready vers `main` et attendre la CI GitHub.

## Definition of Done

- [ ] permission absente du catalogue applicatif et de la base ;
- [ ] migration V86 testée H2/PostgreSQL 16 ;
- [ ] aucune référence active autre qu'historique ;
- [ ] tests complets verts ;
- [ ] documentation et suivi alignés ;
- [ ] PR revue et CI verte.

## Sécurité / rollback

Cette modification retire volontairement une autorité trop large. Le rollback ne doit pas réintroduire automatiquement `HOSPITALIZATION_MANAGE`. En cas de besoin fonctionnel, les permissions dédiées doivent être attribuées explicitement au rôle concerné.

## Impact SemVer

La suppression d'une permission persistée et du comportement d'autorisation associé est un **breaking change du modèle d'autorisation**. Selon `docs/release/SEMANTIC-VERSIONING.md`, l'impact prévu est **MAJOR** lors de la prochaine release qui inclura ce changement. Aucun changement de `VERSION` n'est effectué dans cette tâche tant qu'aucune release n'est préparée.
