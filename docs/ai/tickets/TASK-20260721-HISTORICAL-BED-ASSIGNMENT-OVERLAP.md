# HOS-BED-001-D — Chevauchements historiques des affectations de lit

## Mode d'intervention

Architecture + Engineering discovery, rattachée à `EPIC-0027 / HOS-BED-001-D`.

## Statut

PROPOSED / NON READY — aucun développement engagé.

## Objectif

Interdire que deux périodes d'affectation du même lit se chevauchent, y compris lorsque les deux lignes sont clôturées.

## Problème actuel

V76 protège l'affectation active, V77 valide la chronologie et V78 protège le tenant. Une importation ou une correction rétroactive peut encore créer deux périodes historiques incompatibles sans ligne active.

## Options à prototyper

1. Contrainte d'exclusion PostgreSQL sur une plage `tstzrange` avec GiST.
2. Verrou applicatif et contrôle transactionnel, avec garde SQL complémentaire.
3. Trigger portable uniquement si la stratégie PostgreSQL ne peut pas être retenue.

La décision doit privilégier une garantie base de données, mesurer les impacts H2/tests et documenter l'extension PostgreSQL requise.

## Critères d'acceptation

- Étant donné deux périodes strictement chevauchantes du même lit, lorsque la seconde est insérée, alors la base la refuse.
- Étant donné deux périodes adjacentes où `fin A = début B`, lorsque la seconde est insérée, alors elle est acceptée.
- Étant donné deux lits différents sur la même période, lorsque les affectations sont insérées, alors elles sont acceptées.
- Étant donné des données historiques incohérentes, lorsque le préflight est exécuté, alors elles sont listées sans suppression automatique.
- Étant donné une correction rétroactive autorisée, lorsque la période change, alors la contrainte est réévaluée dans la même transaction.

## Estimation et responsabilité

- Estimation : 3–5 SP, 3–5 jours senior après levée des préconditions.
- Profil recommandé : backend senior + DBA PostgreSQL.
- Reviewer : DBA + lead backend + cadre infirmier/bed manager.
- Tests attendus : migration PostgreSQL 16, bornes adjacentes, concurrence, import et rollback.

## Definition of Ready

- [ ] Docker/Testcontainers ou PostgreSQL 16 dédié disponible.
- [ ] Préflight exécuté sur une copie représentative.
- [ ] Sémantique des bornes temporelles validée par le bed manager.
- [ ] Stratégie GiST/extension validée par le DBA.
- [ ] Plan de réconciliation et rollback documenté.

## Definition of Done

- [ ] documentation fonctionnelle et technique créée ;
- [ ] ADR ajouté si une extension ou une stratégie non portable est retenue ;
- [ ] migration testée sur PostgreSQL 16 ;
- [ ] concurrence et corrections rétroactives couvertes ;
- [ ] aucun historique supprimé automatiquement ;
- [ ] changelog, tracking et plan de déploiement mis à jour.

## Sécurité / régression

Le prototype ne doit exposer aucune donnée patient. Toute réparation historique nécessite validation métier/DBA et audit de l'acteur, du motif, de l'ancienne période et de la nouvelle période.

## Impact version / SemVer

Probable `MINOR` si la contrainte est additive ; `MAJOR` uniquement si un contrat public ou une sémantique publiée doit être retiré. Aucun bump préparé.

## Reste à faire

Lever les cinq critères de Ready avant toute migration ou modification applicative.
