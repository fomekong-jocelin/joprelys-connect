# HOS-RBAC-001-A — Séparer la gestion opérationnelle du statut des lits

> **État actuel** : ce ticket décrit le premier incrément RBAC hospitalier livré. Les mentions de `HOSPITALIZATION_MANAGE` ci-dessous correspondent à l'état transitoire de 001-A. HOS-RBAC-001-B/C ont ensuite séparé les opérations restantes et HOS-RBAC-001-D / #121 supprime définitivement cette permission du catalogue et de la base via V86.

## Métadonnées

- **Epic** : EPIC-0027 — Organisation hospitalière, capacité et parcours patient
- **Story** : HOS-RBAC-001 — Contextualiser les permissions hospitalières
- **Audit** : AUDIT-20260721
- **Écarts réduits** : GAP-006 et GAP-016
- **Priorité** : Critique / phase 0
- **Estimation** : 2 SP / 1 à 2 jours
- **Statut** : QA TECHNIQUE VERTE / VALIDATION RSSI ET MÉTIER REQUISE

## Contexte

Au moment de HOS-RBAC-001-A, l'endpoint `POST /api/spatial/beds/{id}/status` était protégé par `HOSPITALIZATION_MANAGE`. Cette permission très large couvrait également admission, transfert, soins et sortie. Un infirmier standard pouvait donc accéder aux commandes de maintenance ou de remise à disposition d'un lit alors que cette responsabilité devait être distincte.

HOS-BED-002-B avait sécurisé les transitions métier, mais la séparation des responsabilités restait absente.

## Objectif

Introduire une permission dédiée `BED_OPERATIONAL_STATUS_MANAGE` et l'utiliser de manière cohérente dans le backend, le catalogue RBAC et l'interface Angular.

## Règles

1. L'endpoint de changement opérationnel du statut d'un lit exige `BED_OPERATIONAL_STATUS_MANAGE`.
2. La permission n'accorde aucun droit d'admission, de transfert, de soin ou de sortie.
3. Le responsable hospitalisation et l'administrateur clinique la reçoivent par défaut.
4. Le médecin la conservait temporairement dans cet incrément pour compatibilité avec le parcours de l'époque ; les incréments suivants ont resserré la matrice.
5. L'infirmier standard ne la reçoit plus.
6. Les rôles personnalisés peuvent recevoir ou perdre cette permission indépendamment des autres intentions hospitalières.
7. L'interface n'affiche les actions rapides de statut que si la permission dédiée est présente.
8. Les garde-fous HOS-BED-002-B restent obligatoires même pour un utilisateur autorisé.

## Matrice transitoire de 001-A

| Rôle système | `HOSPITALIZATION_MANAGE` à l'époque | `BED_OPERATIONAL_STATUS_MANAGE` | Résultat |
|---|---:|---:|---|
| `ADMIN_CLINIQUE` | Oui | Oui | actions opérationnelles autorisées |
| `MEDECIN` | Oui | Oui, temporaire | compatibilité de l'incrément A |
| `INFIRMIER` | Oui | Non | actions de statut masquées et API refusée |
| `RESPONSABLE_HOSPITALISATION` | Oui | Oui | gestion opérationnelle autorisée |
| autres rôles | selon catalogue de l'époque | Non par défaut | refus sauf rôle personnalisé |

Cette matrice était explicitement transitoire. L'état courant est porté par HOS-RBAC-001-B/C/D et ne contient plus `HOSPITALIZATION_MANAGE` à partir de V86.

## Critères d'acceptation

- [x] La permission est présente dans le catalogue RBAC avec un libellé et une description métier.
- [x] L'endpoint de statut exige exclusivement `BED_OPERATIONAL_STATUS_MANAGE`.
- [x] `INFIRMIER` conserve les autres actions legacy de l'incrément A mais ne reçoit pas la nouvelle permission de statut.
- [x] `MEDECIN`, `RESPONSABLE_HOSPITALISATION` et `ADMIN_CLINIQUE` reçoivent la permission dans cet incrément.
- [x] Angular vérifie la permission dédiée avant d'afficher les actions rapides.
- [x] Le transfert restait protégé par `HOSPITALIZATION_MANAGE` dans cet incrément historique ; HOS-RBAC-001-B l'a depuis remplacé par `HOSPITALIZATION_TRANSFER`.
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

`RbacBootstrap` exécute `RbacStore.seedCatalog()` au démarrage. Les permissions présentes dans le catalogue sont créées ou mises à jour, puis les permissions des rôles système sont resynchronisées.

À partir de HOS-RBAC-001-D, Flyway V86 supprime en plus la permission historique qui ne doit plus exister en base ; `seedCatalog()` ne la recrée pas.

Les utilisateurs concernés doivent renouveler leur contexte d'accès après déploiement afin que l'interface reflète le catalogue courant.

## Risques résiduels actuels

- `HOSPITALIZATION_MANAGE` n'est plus un risque résiduel accepté : HOS-RBAC-001-D le supprime définitivement.
- aucune portée par service ou unité n'est encore évaluée ;
- les habilitations professionnelles et délégations temporelles restent à concevoir ;
- une validation de la matrice par le RSSI/DPO et le cadre reste obligatoire.

## Hors périmètre

- séparation de la sortie médicale, administrative et physique, traitée par les incréments HOS-DIS ;
- permissions contextuelles par unité, relation de soin ou délégation ;
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
