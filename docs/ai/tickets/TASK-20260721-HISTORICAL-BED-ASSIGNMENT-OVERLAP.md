# HOS-BED-001-D — Chevauchements historiques des affectations de lit

## Mode d'intervention

Architecture + Engineering, rattachée à `EPIC-0027 / HOS-BED-001-D`.

## Statut

IMPLEMENTED / QA TECHNIQUE EN COURS — PR empilée #99.

La PR cible temporairement la branche de #98 afin de réutiliser son correctif de validation PostgreSQL sans duplication. Elle devra être retargetée vers `main` après fusion ou reprise du prérequis.

## Objectif

Interdire que deux périodes d'affectation du même lit se chevauchent, y compris lorsque les deux lignes sont clôturées.

## Problème traité

V76 protège l'affectation active, V77 valide la chronologie et V78 protège le tenant. Une importation ou une correction rétroactive pouvait encore créer deux périodes historiques incompatibles sans ligne active.

## Décision retenue

1. Contrainte d'exclusion PostgreSQL GiST sur une plage `tsrange` semi-ouverte `[assigned_at, released_at)`.
2. Extension `btree_gist` pour l'égalité GiST des UUID.
3. Statut d'intégrité `VALID | QUARANTINED` ajouté par V79, portable H2/PostgreSQL.
4. Migration Java V80 active uniquement sous PostgreSQL.
5. V80 échoue si des chevauchements `VALID` sont détectés.
6. Quarantaine exclusivement manuelle, motivée, attribuée et limitée aux affectations clôturées.
7. Aucun historique supprimé ou corrigé automatiquement.

Le schéma actuel utilise `TIMESTAMP WITHOUT TIME ZONE`; `tsrange` est donc cohérent. Le passage futur à `TIMESTAMPTZ`/`tstzrange` relève de GAP-038.

## Critères d'acceptation

- [x] Deux périodes strictement chevauchantes du même lit sont refusées par PostgreSQL.
- [x] Deux périodes adjacentes où `fin A = début B` sont acceptées.
- [x] Deux lits différents sur la même période sont acceptés.
- [x] Le préflight liste les données historiques incompatibles sans suppression automatique.
- [x] Une correction rétroactive qui recrée un overlap est refusée dans la même instruction.
- [x] Une migration avec conflits non arbitrés échoue avant création de la contrainte.
- [x] Une ligne clôturée approuvée peut être conservée comme `QUARANTINED`.
- [x] Une affectation active ne peut pas être mise en quarantaine.

## Estimation et responsabilité

- Estimation initiale : 3–5 SP, 3–5 jours senior.
- Profil : backend senior + DBA PostgreSQL.
- Reviewers : DBA + lead backend + cadre infirmier/bed manager.
- Tests : migration PostgreSQL 16, bornes adjacentes, correction rétroactive, quarantaine, import et rollback.

## Livrables

- `V79__prepare_bed_assignment_overlap_quarantine.sql` ;
- `V80__enforce_historical_bed_assignment_non_overlap.java` ;
- `BedAssignmentOverlapPostgresqlMigrationTest` ;
- `ADR-0003-postgresql-bed-assignment-temporal-exclusion.md` ;
- `FUNCTIONAL-SPEC.md` ;
- `TECHNICAL-DESIGN.md` ;
- `PRE-MIGRATION-CHECKS.sql` ;
- `QUARANTINE-APPROVED-ASSIGNMENTS.sql`.

## Definition of Ready

- [x] Docker/Testcontainers PostgreSQL 16 disponible dans la CI.
- [ ] Préflight exécuté sur une copie représentative anonymisée.
- [ ] Sémantique `[début, fin)` validée par le bed manager.
- [ ] Stratégie GiST/extension validée par le DBA.
- [x] Plan de réconciliation et rollback documenté.

## Definition of Done

- [x] documentation fonctionnelle et technique créée ;
- [x] ADR ajouté pour la stratégie PostgreSQL non portable ;
- [ ] migration et suites complètes vertes sous PostgreSQL 16 ;
- [x] adjacence, overlap, lits différents et corrections rétroactives couverts ;
- [x] aucun historique supprimé automatiquement ;
- [x] plan de déploiement, quarantaine et rollback documenté ;
- [ ] revue DBA et bed manager ;
- [ ] préflight sur copie représentative ;
- [ ] changelog, tracking global et matrice d'audit finalisés après QA verte.

## Sécurité / régression

Le prototype n'expose aucun nom, contact, diagnostic ou donnée clinique. Les scripts restreignent les sorties aux identifiants techniques et périodes nécessaires. Toute quarantaine réelle exige validation métier/DBA, motif, acteur et conservation du rapport de préflight.

## Impact version / SemVer

`MINOR` recommandé : ajout de colonnes, d'une extension PostgreSQL et d'une nouvelle garantie de données. Aucun contrat HTTP public n'est retiré.

## Reste à faire

1. obtenir la CI verte de #99 ;
2. faire valider l'ADR et l'extension par le DBA ;
3. faire signer la sémantique des bornes par le bed manager ;
4. exécuter le préflight sur une copie anonymisée représentative ;
5. retargeter #99 vers `main` après traitement de #98 ;
6. mettre à jour le suivi global après validation.
