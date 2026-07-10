# BUG-20260710-V55-H2-COMPATIBILITY — Migration V55 compatible H2 et PostgreSQL

## Mode

Diagnostic + Engineering — correction de migration Flyway rétrocompatible.

**Statut :** QA VALIDÉE — migration et suite complète validées sur H2 et PostgreSQL 16 réel via Testcontainers.

## Contexte

La migration `V55__billing_amounts_float_to_numeric.sql` aligne les colonnes financières avec le passage Java de `Double` vers `BigDecimal`.

La version initiale utilisait des constructions propres à PostgreSQL :

- cast `::NUMERIC` ;
- clause `USING` dans `ALTER COLUMN` ;
- regroupement de plusieurs changements de type dans une seule instruction.

Cette syntaxe bloquait le démarrage du contexte H2 et provoquait 219 erreurs en cascade dans la suite backend.

## Objectif

Rendre V55 exécutable sur PostgreSQL et H2 sans modifier la sémantique financière, les contrats API, les valeurs existantes ou la précision cible.

## Périmètre inclus

- migration Flyway V55 ;
- test automatisé PostgreSQL 16 avec Testcontainers ;
- contrôle des précisions et échelles dans `information_schema` ;
- documentation fonctionnelle et technique ;
- tests backend et CI.

## Périmètre exclu

- changement des règles de calcul financier ;
- modification des statuts de facture ou de paiement ;
- modification AuthN/AuthZ ou multi-tenant ;
- traitement d’un environnement partagé ayant déjà enregistré l’ancienne empreinte V55.

## Critères d’acceptation

- [x] V55 ne contient plus de cast PostgreSQL `::`.
- [x] V55 ne dépend plus de la clause PostgreSQL `USING`.
- [x] Les types finaux restent `NUMERIC(19,4)` pour les montants et `NUMERIC(5,4)` pour le taux de couverture.
- [x] La remise nulle est normalisée à `0` et le coefficient nul à `1`, comme dans la migration initiale.
- [x] Les migrations Flyway s’exécutent sur H2.
- [x] Toutes les migrations s’exécutent sur PostgreSQL 16 réel via Testcontainers.
- [x] Les 13 colonnes concernées sont contrôlées dans `information_schema`.
- [x] Le test PostgreSQL est obligatoire : il n’est pas ignoré en l’absence de Docker.
- [x] `./mvnw clean verify -B -Dspring.profiles.active=test` réussit.
- [x] Les tests Angular et le build de production réussissent.
- [x] Aucun contrat API, calcul métier ou permission n’est modifié.

## Plan d’action

- [x] Lire les règles IA et les standards du dépôt.
- [x] Confirmer la configuration H2 des tests et de la CI.
- [x] Identifier les constructions SQL incompatibles.
- [x] Vérifier les précisions attendues dans les entités JPA.
- [x] Remplacer les conversions spécifiques PostgreSQL par des opérations portables et atomiques.
- [x] Ajouter Testcontainers JUnit Jupiter et PostgreSQL 2.0.5.
- [x] Ajouter `FlywayPostgresqlMigrationTest` avec PostgreSQL 16.
- [x] Valider H2, PostgreSQL, Maven, Angular et le build de production via la PR temporaire #12.
- [ ] Mettre à jour le changelog et le suivi projet avant fusion.
- [x] Ouvrir la Pull Request dédiée : #8.

## Definition of Done

- [x] Migration portable appliquée.
- [x] Tests backend verts sur H2.
- [x] Test de migration PostgreSQL 16 vert.
- [x] Précisions et échelles V55 vérifiées.
- [x] Documentation et ticket à jour.
- [x] Pull Request dédiée ouverte.
- [ ] Vérifier avant déploiement qu’aucun environnement partagé n’a appliqué l’ancienne empreinte V55.
- [ ] Changelog et suivi projet finalisés avant fusion.

## Estimation et responsabilités

| Champ | Valeur |
|---|---|
| Priorité | P0 |
| Story points | 2 |
| Estimation senior | 0,4 j |
| Estimation intermédiaire | 0,7 j |
| Estimation junior | 1,2 j |
| Profil recommandé | Backend Java / SQL senior |
| Reviewer | Lead Backend + référent données |
| Sprint | SPRINT-0014 |

## Résultats de validation

Validation finale sur la PR temporaire #12 :

- Flyway V55 sur H2 : ✅
- Flyway complet sur PostgreSQL 16 Testcontainers : ✅
- Contrôle des 13 colonnes `NUMERIC` : ✅
- Backend Maven strict : ✅
- Suite backend complète : ✅
- Tests Angular : ✅
- Build Angular production : ✅

## Sécurité et régression

- Aucun endpoint, rôle ou contrôle multi-tenant n’est modifié.
- Les précisions cibles sont contrôlées directement dans PostgreSQL.
- Les seules normalisations de données correspondent aux `COALESCE` de la version initiale.
- Le test PostgreSQL devient une barrière de non-régression pour toutes les migrations Flyway.

## Impact version

**PATCH** — correction rétrocompatible de migration et renforcement des tests de base de données.

## Références

- PostgreSQL `ALTER TABLE ... ALTER COLUMN ... SET DATA TYPE`.
- Testcontainers JUnit Jupiter et module PostgreSQL 2.0.5.
- Configuration H2 : `backend/src/test/resources/application-test.yml`.
- CI backend : `.github/workflows/ci.yml`.

## Reste à faire

Finaliser le changelog et le suivi projet, puis vérifier l’historique Flyway des environnements partagés avant fusion/déploiement.
