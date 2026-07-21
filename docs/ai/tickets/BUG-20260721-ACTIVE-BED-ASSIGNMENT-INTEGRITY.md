# BUG-20260721 — Intégrité des affectations actives de lit

## Mode d'intervention

Engineering + correction P0 d'intégrité, rattachée à `EPIC-0027 / HOS-BED-001-A`.

## Statut

QA H2 VERTE / VALIDATION POSTGRESQL REQUISE — implémentation et suite backend terminées le 2026-07-21 ; le test PostgreSQL 16 reste à exécuter avec Docker.

## Objectif

Garantir en base qu'un lit ne possède jamais plus d'une affectation active, y compris lorsqu'une écriture contourne le claim applicatif normal.

## Constat et preuve

`BedRepository.claimIfFree` protège le chemin normal d'admission et de transfert par une mise à jour atomique du statut. La table `bed_assignments` ne possède toutefois qu'un index non unique sur `bed_id`. Deux lignes avec `released_at IS NULL` restent donc techniquement possibles par import, écriture SQL, désynchronisation du statut ou futur code concurrent.

## Périmètre inclus

- migration Flyway portable H2/PostgreSQL ajoutant un marqueur technique de lit actif ;
- contrainte de cohérence entre `released_at`, `bed_id` et ce marqueur ;
- index unique empêchant deux affectations actives du même lit ;
- synchronisation du marqueur par l'entité JPA ;
- traduction d'une collision tardive en HTTP 409 par un service applicatif dédié ;
- tests du domaine, de la base, de la migration et des chemins admission/transfert ;
- requête de préflight documentée, sans correction destructive.

## Périmètre exclu

- interdiction des chevauchements entre périodes déjà clôturées ;
- clé étrangère de `hospitalization_id` vers `hospitalizations` ;
- unicité de l'affectation active par hospitalisation ;
- refonte de la sortie physique ou suppression de l'endpoint générique de statut ;
- réservation anticipée et idempotence complète des commandes.

## Règles métier

1. Une affectation est active lorsque `released_at IS NULL`.
2. Un lit ne possède au maximum qu'une affectation active.
3. Une affectation clôturée conserve son historique et ne monopolise plus le lit.
4. Le marqueur technique actif vaut `bed_id` pour une affectation active et `NULL` pour une affectation clôturée.
5. Une collision détectée lors d'une admission ou d'un transfert renvoie HTTP 409 et provoque le rollback de la transaction.
6. Les doublons historiques ne sont jamais supprimés ou fusionnés automatiquement par la migration.

## Critères d'acceptation

- Étant donné une affectation active d'un lit, lorsque la base reçoit une seconde affectation active du même lit, alors l'écriture est refusée.
- Étant donné une affectation clôturée, lorsqu'une nouvelle affectation active est créée sur le même lit, alors l'écriture réussit.
- Étant donné une ligne active dont le marqueur est nul ou différent du lit, lorsque l'écriture est exécutée, alors la contrainte de cohérence la refuse.
- Étant donné une collision tardive dans une commande métier, lorsque l'affectation est persistée, alors l'API renvoie 409 et aucun claim partiel n'est validé.
- Étant donné des doublons historiques, lorsque le préflight est exécuté, alors ils sont listés et le déploiement est arrêté pour correction supervisée.

## Estimation et responsabilité

- Estimation : 3 SP, 2 jours senior incluant migration, documentation et tests.
- Profil recommandé : backend senior + DBA.
- Reviewer : lead backend + DBA + cadre infirmier/bed manager.
- Sprint : incrément P0 de SPRINT-0014 ; aucun engagement du reste de HOS-BED-001.

## Definition of Ready

- [x] risque GAP-005/RISK-009 documenté ;
- [x] modèle actuel et tous les points d'écriture identifiés ;
- [x] stratégie portable H2/PostgreSQL définie ;
- [x] préflight non destructif spécifié ;
- [x] tests attendus identifiés.

## Actions

- [x] Analyser schéma, transactions et points d'écriture.
- [x] Créer spécification, conception, contrat API, modèle de données et plan de test.
- [x] Ajouter la migration Flyway et la synchronisation JPA.
- [x] Centraliser la création d'affectation active et traduire les collisions en 409.
- [x] Ajouter les tests de contrainte, libération et collision applicative.
- [x] Étendre le test de migration PostgreSQL.
- [x] Exécuter les vérifications ciblées et complètes disponibles.
- [x] Mettre à jour changelog, tracking, planning, backlog et registre des risques.

## Tests / vérifications

- `mvn -DskipTests compile` : compilation des 633 classes de production réussie.
- Tests ciblés `ActiveBedAssignmentServiceTest`, `BedAssignmentRepositoryTest`, `HospitalizationAdmissionServiceTest` et `SpatialControllerTest` : 20 tests réussis.
- `mvn clean verify` : 486 tests, 0 échec, 0 erreur, 1 test ignoré.
- Flyway H2 : toutes les migrations appliquées jusqu'à V76 et validation Hibernate réussie.
- Le test `FlywayPostgresqlMigrationTest` contient les assertions V76 (colonne, CHECK, index unique et collision réelle), mais a été ignoré car Docker/Testcontainers n'est pas disponible sur le poste.
- `git diff --check` : aucune erreur d'espacement ; avertissements CRLF historiques uniquement.

## Checklist de review

- [x] Contrainte portée par la base et non uniquement par le frontend ou le service.
- [x] Chemin normal conservé : claim atomique puis persistance dans la même transaction.
- [x] Collision tardive traduite en 409 sans identité concurrente exposée.
- [x] Rollback du transfert vérifié lorsque le statut `FREE` est désynchronisé d'une affectation active.
- [x] Historique clôturé conservé et lit réutilisable.
- [x] Aucun `DELETE` ou arbitrage automatique des doublons historiques.
- [ ] Préflight exécuté sur une copie récente de la base de production.
- [ ] Testcontainers PostgreSQL 16 vert.
- [ ] Revue DBA et bed manager signée.

## Sécurité / régression

- Aucun élargissement de permission ou exposition de donnée patient.
- Migration additive mais bloquante si des doublons actifs existent : préflight obligatoire avant production.
- Le rollback transactionnel doit couvrir le claim du lit, le séjour et l'affectation.
- Aucun changement frontend, Flutter ou configuration.

## Impact version / SemVer

Durcissement compatible du comportement et migration additive : cible `MINOR` probable (`0.11.0`). Aucun bump ni release n'est préparé dans cette intervention.

## Reste à faire

Exécuter le préflight, le test PostgreSQL 16 et la revue DBA/bed manager avant tout déploiement. La FK séjour, l'unicité active par hospitalisation et la chronologie sont couvertes par HOS-BED-001-B ; la cohérence tenant est couverte par HOS-BED-001-C sous H2. Les chevauchements de périodes clôturées restent dans HOS-BED-001-D proposé.
