# BUG-20260721 — Intégrité séjour et période des affectations de lit

## Mode d'intervention

Engineering + correction P0 d'intégrité, rattachée à `EPIC-0027 / HOS-BED-001-B`.

## Statut

QA H2 VERTE / VALIDATION POSTGRESQL REQUISE — incrément borné réalisé après HOS-BED-001-A.

## Objectif

Garantir qu'une affectation référence un séjour existant, qu'un séjour ne possède qu'une affectation active et qu'une période clôturée n'a jamais une fin antérieure à son début.

## Constat et preuve

`bed_assignments.hospitalization_id` est actuellement un UUID sans clé étrangère. Le repository retourne une affectation active par hospitalisation sous forme d'`Optional`, alors que la base autorise plusieurs lignes actives. Enfin, aucun CHECK n'interdit `released_at < assigned_at`.

## Périmètre inclus

- migration Flyway V77 compatible H2/PostgreSQL ;
- FK `hospitalization_id → hospitalizations.id` avec `ON DELETE RESTRICT` ;
- marqueur et index unique pour l'affectation active par hospitalisation ;
- CHECK de cohérence du marqueur et CHECK chronologique ;
- synchronisation JPA des deux marqueurs actifs ;
- commande de clôture explicite refusant une date inversée ;
- préflights non destructifs des orphelins, doublons actifs et périodes invalides ;
- tests H2, PostgreSQL préparé, domaine et non-régression admission/transfert/sortie.

## Périmètre exclu

- contrainte d'exclusion des chevauchements entre périodes clôturées d'un même lit ;
- cohérence tenant composite entre affectation, séjour et lit ;
- refonte sortie médicale/physique ;
- suppression de l'endpoint générique de statut ;
- réservation anticipée et idempotence complète.

## Règles métier

1. Toute affectation référence un séjour existant.
2. La suppression d'un séjour référencé est interdite ; son historique doit être archivé, jamais détruit.
3. Un séjour possède zéro ou une affectation active.
4. Une affectation active possède `active_hospitalization_id = hospitalization_id`.
5. Une affectation clôturée possède des marqueurs actifs nuls.
6. `released_at` est nul ou supérieur ou égal à `assigned_at`.
7. Une collision tardive renvoie HTTP 409 et annule la transaction.
8. La migration n'invente aucune date et ne rattache aucun orphelin automatiquement.

## Critères d'acceptation

- Étant donné un identifiant de séjour inexistant, lorsque l'affectation est insérée, alors la base refuse l'écriture.
- Étant donné un séjour avec une affectation active, lorsqu'une seconde affectation active est créée, alors la base la refuse.
- Étant donné une affectation clôturée, lorsqu'une nouvelle affectation du même séjour est créée, alors elle est acceptée si le workflow le permet.
- Étant donné une fin antérieure au début, lorsque la clôture est demandée ou persistée, alors elle est refusée.
- Étant donné un séjour référencé, lorsque sa suppression est tentée, alors la base la refuse et les mouvements restent présents.

## Estimation et responsabilité

- Estimation : 3 SP, 2 jours senior.
- Profil recommandé : backend senior + DBA.
- Reviewer : lead backend + DBA + cadre infirmier/bed manager.
- Sprint : incrément P0 de SPRINT-0014 ; aucun engagement du reste de HOS-BED-001.

## Definition of Ready

- [x] cascades et suppressions analysées ;
- [x] absence d'API de suppression de séjour confirmée ;
- [x] stratégie `ON DELETE RESTRICT` retenue pour préserver l'historique ;
- [x] préflights et tests définis ;
- [x] périmètre tenant/chevauchement exclu explicitement.

## Actions

- [x] Analyser schéma, cascades, points d'écriture et suppressions.
- [x] Créer la documentation initiale.
- [x] Ajouter la migration V77.
- [x] Adapter l'entité et les commandes de clôture.
- [x] Adapter les fixtures qui créent des affectations orphelines.
- [x] Ajouter les tests H2 et PostgreSQL.
- [x] Exécuter les vérifications ciblées et complètes disponibles.
- [x] Mettre à jour backlog, tracking, changelog, planning et risques.

## Tests / vérifications

- Tests ciblés intégrité/admission/transfert/sortie : 31 tests, 0 échec, 1 test PostgreSQL ignoré faute de Docker.
- Tests ciblés d'isolation des fixtures : 45 tests puis 13 tests, tous verts.
- `mvn clean verify` : 490 tests, 0 échec, 0 erreur, 1 test ignoré ; JAR Spring Boot construit.
- Flyway H2 : migrations appliquées jusqu'à V77 et validation Hibernate réussie.
- PostgreSQL 16 : assertions de schéma et de collisions préparées dans Testcontainers, non exécutées sur ce poste faute de moteur Docker.
- Préflight : `PRE-MIGRATION-CHECKS.sql` fournit trois contrôles en lecture seule, à exécuter avant V77.
- `git diff --check` : aucune erreur d'espacement ; avertissements CRLF historiques uniquement.

## Checklist de review

- [x] FK et contraintes portées par la base, pas uniquement par le service.
- [x] Suppression restrictive et absence de réparation automatique documentées.
- [x] Clôture source flushée avant l'affectation cible, dans une transaction unique.
- [x] Orphelin, double affectation active, période inversée et suppression du séjour couverts sous H2.
- [x] Fixtures nettoyées dans l'ordre mouvement → séjour → patient.
- [x] Aucun élargissement de permission, changement d'API ou donnée sensible supplémentaire.
- [ ] Préflight exécuté sur une copie récente de production.
- [ ] Testcontainers PostgreSQL 16 vert.
- [ ] Revues DBA, bed manager et DPO signées.

## Sécurité / régression

- Aucun changement de permission ou de donnée exposée.
- La FK bloque les purges historiques qui comptaient sur la cascade patient ; la suite complète l'a confirmé et les fixtures concernées purgent maintenant explicitement les mouvements avant les séjours. Ce comportement de production est intentionnel et reste à valider par le DPO/DBA.
- La fusion patient existante réaffecte les hospitalisations au dossier principal puis conserve le dossier secondaire en statut `MERGED` ; elle ne dépend pas d'une suppression destructive et reste compatible avec la FK.
- La migration échoue volontairement si le préflight révèle des données incompatibles.
- Aucun changement frontend, mobile ou configuration.

## Impact version / SemVer

Durcissement additif du schéma et des erreurs métier : cible `MINOR` probable (`0.11.0`). Aucun bump ni release n'est préparé.

## Reste à faire

Exécuter le préflight, Testcontainers PostgreSQL 16 et la revue DBA/bed manager/DPO avant déploiement. La cohérence tenant composite est désormais couverte par HOS-BED-001-C sous H2 ; le contrôle complet des chevauchements reste à arbitrer séparément.
