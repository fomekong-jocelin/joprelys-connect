# BUG-20260721 — Cohérence établissement des affectations de lit

## Mode d'intervention

Engineering + correction P0 d'intégrité, rattachée à `EPIC-0027 / HOS-BED-001-C`.

## Statut

QA H2 VERTE / VALIDATION POSTGRESQL REQUISE — incrément borné réalisé après HOS-BED-001-A/B.

## Objectif

Garantir qu'une affectation, son séjour et son lit appartiennent au même établissement, y compris lorsqu'une écriture contourne les services applicatifs.

## Constat et preuve

`bed_assignments`, `hospitalizations` et `beds` portent chacune un `organization_id`, mais les clés étrangères actuelles ne comparent que les identifiants techniques. Une ligne peut donc référencer un séjour ou un lit d'un autre tenant si elle est créée par import, SQL direct ou futur code défectueux.

## Périmètre inclus

- migration Flyway V78 compatible H2/PostgreSQL ;
- clés candidates `(id, organization_id)` sur `hospitalizations` et `beds` ;
- FK composite affectation → séjour du même établissement ;
- FK composite affectation → lit du même établissement ;
- garde applicative refusant un lit dont l'établissement diffère du contexte d'affectation ;
- préflight non destructif des incohérences existantes ;
- tests H2, service, admission/transfert et assertions PostgreSQL préparées.

## Périmètre exclu

- chevauchements entre périodes clôturées ;
- FK tenant de l'ensemble des tables hospitalières legacy ;
- refonte du contexte tenant Hibernate ;
- organisation multi-établissement cible d'ADR-0002 ;
- sortie physique et réservation anticipée.

## Règles métier

1. `bed_assignments.organization_id` est identique à celui du séjour référencé.
2. `bed_assignments.organization_id` est identique à celui du lit référencé.
3. Une collision tenant est refusée avant écriture lorsqu'elle est détectable par le service.
4. La base reste le dernier garde-fou pour les imports et écritures concurrentes.
5. Une incohérence historique bloque V78 ; aucune réaffectation automatique n'est autorisée.
6. Les suppressions du séjour et du lit restent restrictives afin de préserver l'historique.

## Critères d'acceptation

- Étant donné un séjour et un lit du même établissement, lorsque l'affectation est créée, alors elle est acceptée.
- Étant donné un lit d'un autre établissement, lorsque le service crée l'affectation, alors il répond 409 avant persistance.
- Étant donné une écriture SQL avec un `organization_id` différent de celui des parents, lorsqu'elle est exécutée, alors la base la refuse.
- Étant donné une base contenant une incohérence tenant, lorsque le préflight est exécuté, alors la ligne est listée sans modification.
- Étant donné une suppression d'un parent référencé, lorsque l'opération est tentée, alors l'historique reste protégé.

## Estimation et responsabilité

- Estimation : 3 SP, 2 jours senior.
- Profil recommandé : backend senior + DBA.
- Reviewer : lead backend + DBA + RSSI/DPO + bed manager.
- Sprint : incrément P0 de SPRINT-0014 ; aucun engagement des chevauchements historiques.

## Definition of Ready

- [x] colonnes tenant des trois tables identifiées ;
- [x] cascades et suppressions analysées ;
- [x] stratégie de FK composite portable retenue ;
- [x] PostgreSQL indisponible localement signalé ;
- [x] périmètre des chevauchements exclu.

## Actions

- [x] Analyser le schéma, les chemins d'écriture et les cascades.
- [x] Créer la documentation initiale.
- [x] Ajouter la migration V78.
- [x] Ajouter la garde applicative.
- [x] Étendre les tests H2 et PostgreSQL.
- [x] Exécuter les suites ciblées et complètes.
- [x] Mettre à jour backlog, tracking, changelog, planning et risques.

## Tests / vérifications

- Tests ciblés service/repository : 12 tests, tous verts.
- Tests ciblés migration/admission/transfert/sortie : 33 tests, 0 échec, 1 test PostgreSQL ignoré faute de Docker.
- `mvn clean verify` : 493 tests, 0 échec, 0 erreur, 1 test ignoré ; JAR Spring Boot construit.
- Flyway H2 : migrations appliquées jusqu'à V78 et validation Hibernate réussie.
- PostgreSQL 16 : assertions des quatre contraintes et du refus cross-tenant préparées, non exécutées faute de moteur Docker.
- Préflight : deux requêtes en lecture seule recherchent les divergences affectation/séjour et affectation/lit.
- `git diff --check` : aucune erreur d'espacement ; avertissements CRLF historiques uniquement.

## Checklist de review

- [x] Isolation tenant garantie par deux FK composites en base.
- [x] Refus applicatif exécuté avant tout appel repository.
- [x] Message 409 sans identifiant ni donnée de l'établissement concurrent.
- [x] `organization_id` de l'hospitalisation renseigné explicitement par le use case.
- [x] Suppressions parentales toujours restrictives.
- [x] Aucun changement d'autorisation, de payload ou de configuration.
- [ ] Préflight exécuté sur une copie récente de production.
- [ ] Testcontainers PostgreSQL 16 vert.
- [ ] Revues DBA, RSSI/DPO et bed manager signées.

## Sécurité / régression

- Le changement renforce l'isolation tenant sans élargir les permissions.
- Les erreurs ne doivent exposer aucun identifiant d'un autre établissement.
- Le use case d'admission renseigne désormais explicitement le tenant sur l'entité, sans dépendre de l'injection implicite de `@TenantId`.
- La migration échoue volontairement sur les données incompatibles.
- Aucun changement frontend, mobile, API publique ou configuration.

## Impact version / SemVer

Durcissement additif du schéma et du comportement : cible `MINOR` probable (`0.11.0`). Aucun bump ni release n'est préparé.

## Reste à faire

Exécuter le préflight, Testcontainers PostgreSQL 16 et les revues DBA/RSSI-DPO/bed manager avant déploiement. Les chevauchements entre périodes clôturées restent à arbitrer séparément.
