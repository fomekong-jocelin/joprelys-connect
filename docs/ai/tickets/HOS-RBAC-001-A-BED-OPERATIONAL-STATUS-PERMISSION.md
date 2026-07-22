# HOS-RBAC-001-A — Séparer la gestion opérationnelle du statut des lits

## Métadonnées

- **Epic** : EPIC-0027 — Organisation hospitalière, capacité et parcours patient
- **Story** : HOS-RBAC-001 — Contextualiser les permissions hospitalières
- **Audit** : AUDIT-20260721
- **Écarts réduits** : GAP-006 et GAP-016
- **Priorité** : Critique / phase 0
- **Estimation** : 2 SP / 1 à 2 jours
- **Statut** : QA TECHNIQUE VERTE / VALIDATION RSSI ET MÉTIER REQUISE

## Contexte

L'endpoint `POST /api/spatial/beds/{id}/status` était protégé par `HOSPITALIZATION_MANAGE`. Cette permission très large couvre également admission, transfert, soins et sortie. Un infirmier standard pouvait donc accéder aux commandes de maintenance ou de remise à disposition d'un lit alors que cette responsabilité doit être distincte.

HOS-BED-002-B a sécurisé les transitions métier, mais la séparation des responsabilités restait absente.

## Objectif

Introduire une permission dédiée `BED_OPERATIONAL_STATUS_MANAGE` et l'utiliser de manière cohérente dans le backend, le catalogue RBAC et l'interface Angular.

## Règles

1. L'endpoint de changement opérationnel du statut d'un lit exige `BED_OPERATIONAL_STATUS_MANAGE`.
2. La permission n'accorde aucun droit d'admission, de transfert, de soin ou de sortie.
3. Le responsable hospitalisation et l'administrateur clinique la reçoivent par défaut.
4. Le médecin la conserve temporairement pour compatibilité avec le parcours actuel.
5. L'infirmier standard ne la reçoit plus.
6. Les rôles personnalisés peuvent recevoir ou perdre cette permission indépendamment de `HOSPITALIZATION_MANAGE`.
7. L'interface n'affiche les actions rapides de statut que si la permission dédiée est présente.
8. Les garde-fous HOS-BED-002-B restent obligatoires même pour un utilisateur autorisé.

## Matrice transitoire

| Rôle système | `HOSPITALIZATION_MANAGE` | `BED_OPERATIONAL_STATUS_MANAGE` | Résultat |
|---|---:|---:|---|
| `ADMIN_CLINIQUE` | Oui | Oui | actions opérationnelles autorisées |
| `MEDECIN` | Oui | Oui, temporaire | compatibilité conservée |
| `INFIRMIER` | Oui | Non | actions de statut masquées et API refusée |
| `RESPONSABLE_HOSPITALISATION` | Oui | Oui | gestion opérationnelle autorisée |
| autres rôles | selon catalogue | Non par défaut | refus sauf rôle personnalisé |

Cette matrice n'est pas la cible finale. Les futurs incréments distingueront hygiène, maintenance, transfert, décision médicale et sortie administrative/physique.

## Critères d'acceptation

- [x] La permission est présente dans le catalogue RBAC avec un libellé et une description métier.
- [x] L'endpoint de statut exige exclusivement `BED_OPERATIONAL_STATUS_MANAGE`.
- [x] `INFIRMIER` conserve les autres actions legacy d'hospitalisation mais ne reçoit pas la nouvelle permission.
- [x] `MEDECIN`, `RESPONSABLE_HOSPITALISATION` et `ADMIN_CLINIQUE` reçoivent la permission.
- [x] Angular vérifie la permission dédiée avant d'afficher les actions rapides.
- [x] Le transfert reste protégé par `HOSPITALIZATION_MANAGE` dans cet incrément.
- [x] Les tests backend verrouillent le catalogue et l'annotation de sécurité.
- [x] Le test Angular verrouille le code de permission utilisé par l'écran.

## Tests ajoutés ou étendus

- `RbacCatalogBedOperationalStatusPermissionTest` ;
- `SpatialControllerAuthorizationTest` ;
- `spatial-management-page.component.spec.ts`.

## Validation automatisée

CI `Joprelys Connect — CI Pipeline`, run **918** :

- backend Maven `clean verify` strict : succès ;
- tests catalogue et annotations RBAC : succès ;
- migrations H2 et PostgreSQL 16 : succès ;
- tests Angular : succès ;
- build Angular production : succès.

## Déploiement

`RbacBootstrap` exécute `RbacStore.seedCatalog()` au démarrage. La permission est donc créée ou mise à jour, puis les permissions des rôles système sont resynchronisées. Les rôles personnalisés ne sont pas modifiés automatiquement.

Les utilisateurs concernés doivent renouveler leur contexte d'accès après déploiement afin que l'interface reflète immédiatement le nouveau catalogue.

## Risques résiduels

- le médecin conserve temporairement la permission ;
- `HOSPITALIZATION_MANAGE` reste encore trop large pour admission, transfert, soins et sortie ;
- aucune portée par service ou unité n'est encore évaluée ;
- hygiène et maintenance ne sont pas encore séparées ;
- aucune délégation temporelle n'existe ;
- une validation de la matrice par le RSSI/DPO et le cadre reste obligatoire.

## Hors périmètre

- séparation de la sortie médicale, administrative et physique ;
- permissions contextuelles par unité, relation de soin ou délégation ;
- création de rôles hygiène et biomédical ;
- séparation du transfert de lit ;
- refonte complète du statut multi-axes du lit.

## Definition of Done

- [x] permission cataloguée ;
- [x] rôles système ajustés ;
- [x] backend protégé ;
- [x] interface alignée ;
- [x] tests ciblés ajoutés ;
- [x] CI complète verte ;
- [ ] validation RSSI/DPO et cadre/bed manager ;
- [ ] matrice d'audit finalisée après validation externe.
