# HOS-RBAC-001-D — Suppression définitive de `HOSPITALIZATION_MANAGE`

## Métadonnées

- **Epic** : EPIC-0027 — Organisation hospitalière, capacité et parcours patient
- **Story** : HOS-RBAC-001 — Autorisations hospitalières contextuelles
- **Issue GitHub** : #121
- **PR** : #122
- **Dépendance livrée** : HOS-RBAC-001-C / PR #107
- **Priorité** : P0 sécurité / phase de développement
- **Type** : refactoring sécurité + migration DB de nettoyage
- **Profil recommandé** : Senior backend sécurité + DBA/reviewer sécurité
- **Estimation** : 2 SP / 0,5 à 1 j senior
- **Reviewer** : Tech Lead + RSSI/DBA
- **Statut** : IN_REVIEW — CI #1027 VERTE — alignement CHANGELOG/backlog et revue humaine requis avant merge

## Contexte

HOS-RBAC-001-C a remplacé les écritures hospitalières génériques par des permissions orientées intention. `HOSPITALIZATION_MANAGE` n'est plus utilisé par les endpoints ni distribué aux rôles système concernés, mais restait catalogué uniquement pour faciliter une rétro-compatibilité des rôles personnalisés.

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

- [x] `RbacCatalog.permissionCodes()` ne contient plus `HOSPITALIZATION_MANAGE`.
- [x] Aucun rôle système ne contient cette permission.
- [x] Aucun endpoint backend n'utilise cette permission.
- [x] Aucun écran Angular n'utilise cette permission comme fallback.
- [x] V86 supprime `HOSPITALIZATION_MANAGE` de `permissions`.
- [x] Les liens `role_permissions` associés disparaissent via la FK `ON DELETE CASCADE`.
- [x] Aucun mapping automatique vers les permissions HOS-RBAC-001-B/C n'est effectué.
- [x] Un test empêche la réintroduction de la permission dans le catalogue.
- [x] Un test PostgreSQL 16 valide un upgrade V85 → V86 avec rôle personnalisé lié à la permission legacy.
- [x] Le greenfield Flyway courant inclut V86 dans la suite Maven stricte ; aucun échec de migration observé en CI #1027.
- [x] La suite Maven complète est verte en CI #1027.
- [x] La suite Angular concernée et le build production sont verts en CI #1027.
- [ ] Toutes les documentations décrivant l'état courant sont alignées : `CHANGELOG.md` et le backlog EPIC-0027 restent à réconcilier sans perte de contenu.

## Action plan

- [x] Rechercher toutes les références `HOSPITALIZATION_MANAGE` dans le dépôt.
- [x] Vérifier qu'aucun endpoint ou composant actif ne dépend encore de cette permission.
- [x] Vérifier la FK `role_permissions.permission_code -> permissions.code ON DELETE CASCADE`.
- [x] Retirer la permission du catalogue Java.
- [x] Ajouter Flyway V86 de suppression fail-closed.
- [x] Ajouter/adapter les tests catalogue, migration et frontend stale-authority.
- [x] Aligner API contract, matrice d'audit, HOS-RBAC-001-A/B/C et les documents techniques concernés.
- [x] Mettre à jour `PROJECT-TRACKING.md`.
- [ ] Mettre à jour `CHANGELOG.md` de façon ciblée sans tronquer son historique.
- [ ] Réconcilier `docs/pm/backlog/EPIC-0027-hospital-organization-capacity-patient-flow.md` avec l'état #100/#102/#107/#121 sans écraser les autres stories.
- [x] Exécuter `./mvnw clean verify` via la CI stricte : succès, job Backend #1027.
- [x] Exécuter tests Angular + build production via la CI : succès, job Frontend #1027.
- [x] Créer la PR #122 Ready vers `main` et obtenir la CI GitHub verte.
- [ ] Revue humaine Tech Lead + RSSI/DBA avant merge.

## Validation CI #1027

Workflow : `Joprelys Connect — CI Pipeline`

- `Detect changed stacks` : **success** ;
- `Frontend Angular — Build & Tests` : **success** ;
- `Run tests` Angular : **success** ;
- `Build Angular production` : **success** ;
- `Backend — Maven Build & Tests` : **success** ;
- `Build and verify (Maven strict)` : **success**.

Le test PostgreSQL `LegacyHospitalizationPermissionPostgresqlMigrationTest` fait partie de la suite backend. Il migre une base PostgreSQL 16 jusqu'à V85, injecte un rôle personnalisé lié à la permission legacy, applique V86 puis vérifie la suppression de la permission et du lien sans suppression du rôle ni attribution automatique de permissions de remplacement. `RbacStore.seedCatalog()` est ensuite exécuté pour vérifier que la permission supprimée n'est pas recréée.

## Definition of Done

- [x] permission absente du catalogue applicatif et supprimée par V86 en base ;
- [x] migration V86 couverte H2/greenfield via la suite et PostgreSQL 16 via le test d'upgrade dédié ;
- [x] aucune référence active dans un endpoint ou composant de production ;
- [x] tests backend/frontend complets verts ;
- [ ] documentation et suivi intégralement alignés ;
- [ ] PR revue par Tech Lead + RSSI/DBA ;
- [x] CI GitHub verte.

## Sécurité / rollback

Cette modification retire volontairement une autorité trop large. Le rollback ne doit pas réintroduire automatiquement `HOSPITALIZATION_MANAGE`. En cas de besoin fonctionnel, les permissions dédiées doivent être attribuées explicitement au rôle concerné.

## Impact SemVer

La suppression d'une permission persistée et du comportement d'autorisation associé est un **breaking change du modèle d'autorisation**. Selon `docs/release/SEMANTIC-VERSIONING.md`, l'impact prévu est **MAJOR** lors de la prochaine release qui inclura ce changement. Aucun changement de `VERSION` n'est effectué dans cette tâche tant qu'aucune release n'est préparée.
