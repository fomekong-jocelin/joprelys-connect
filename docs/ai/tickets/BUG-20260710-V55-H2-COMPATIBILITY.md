# BUG-20260710-V55-H2-COMPATIBILITY — Migration V55 incompatible avec H2

## Mode

Diagnostic + Engineering — correction de migration Flyway rétrocompatible.

**Statut :** QA — correction implémentée et validée sur H2 ; validation PostgreSQL réelle restante avant livraison partagée.

## Contexte

La migration `V55__billing_amounts_float_to_numeric.sql` aligne les colonnes financières avec le passage Java de `Double` vers `BigDecimal`.

La production cible PostgreSQL, tandis que les tests backend et la CI exécutent Flyway sur H2. La migration initiale utilisait des constructions propres à PostgreSQL :

- cast `::NUMERIC` ;
- clause `USING` dans `ALTER COLUMN` ;
- regroupement de plusieurs changements de type dans une seule instruction.

Cette syntaxe bloquait le démarrage du contexte de test et provoquait 219 erreurs en cascade dans la suite backend.

## Objectif

Rendre V55 exécutable sur PostgreSQL et H2 sans modifier la sémantique financière, les contrats API, les valeurs existantes ou la précision cible.

## Périmètre inclus

- migration Flyway V55 ;
- documentation fonctionnelle et technique de la migration ;
- vérification de la conversion des colonnes financières ;
- tests backend et CI.

## Périmètre exclu

- changement des règles de calcul financier ;
- modification des DTO ou entités BigDecimal ;
- refonte du moteur de tests vers Testcontainers ;
- modification des statuts de facture ou de paiement.

## Critères d’acceptation

- [x] V55 ne contient plus de cast PostgreSQL `::`.
- [x] V55 ne dépend plus de la clause PostgreSQL `USING`.
- [x] Les types finaux restent `NUMERIC(19,4)` pour les montants et `NUMERIC(5,4)` pour le taux de couverture.
- [x] Les valeurs nulles historiquement normalisées conservent le même comportement : remise à `0` et coefficient à `1`.
- [x] Les migrations Flyway s’exécutent sur H2 utilisé par les tests.
- [x] La syntaxe utilise la forme standard `ALTER COLUMN ... SET DATA TYPE`, acceptée par PostgreSQL.
- [x] `./mvnw clean verify -B -Dspring.profiles.active=test` réussit dans la validation combinée.
- [x] Aucun contrat API, calcul métier ou permission n’est modifié.
- [ ] Exécution contrôlée de V55 sur une base PostgreSQL de test ou Testcontainers avant livraison en environnement partagé.

## Plan d’action

- [x] Lire les règles IA et les standards du dépôt.
- [x] Confirmer la configuration H2 des tests et de la CI.
- [x] Identifier les constructions SQL incompatibles.
- [x] Vérifier la précision attendue dans les entités JPA.
- [x] Documenter la stratégie fonctionnelle et technique.
- [x] Remplacer les conversions spécifiques PostgreSQL par des opérations SQL portables et atomiques.
- [x] Vérifier le diff pour exclure tout changement métier.
- [x] Exécuter les tests backend via la PR temporaire de validation #11.
- [ ] Mettre à jour le changelog et le suivi projet avant fusion.
- [x] Ouvrir une Pull Request dédiée : #8.

## Definition of Ready

- [x] Cause technique identifiée.
- [x] Périmètre limité à V55 et à sa documentation.
- [x] Critères d’acceptation définis.
- [x] Risques de données identifiés.

## Definition of Done

- [x] Migration portable appliquée.
- [x] Tests backend verts sur H2.
- [x] Documentation et ticket à jour.
- [x] Pull Request dédiée ouverte.
- [ ] Validation PostgreSQL réelle réalisée.
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

Validation combinée sur la PR temporaire #11 :

- Flyway V55 sur H2 : ✅
- Backend Maven strict : ✅
- Tests backend : ✅ 276 tests
- Tests Angular : ✅
- Build Angular production : ✅

## Sécurité et régression

- Aucun endpoint, rôle ou contrôle multi-tenant n’est modifié.
- Risque principal : arrondi ou perte de valeur durant la conversion FLOAT8 vers NUMERIC.
- Mesure : précisions conservées et normalisation limitée aux colonnes déjà couvertes par les `COALESCE` initiaux.
- Risque restant : comportement sur une base PostgreSQL contenant des valeurs atypiques ; une validation dédiée reste obligatoire.

## Impact version

- **PATCH** : correction rétrocompatible d’une migration non fonctionnelle dans l’environnement de test.

## Références

- PostgreSQL `ALTER TABLE ... ALTER COLUMN ... SET DATA TYPE`.
- Configuration H2 : `backend/src/test/resources/application-test.yml`.
- CI backend : `.github/workflows/ci.yml`.

## Reste à faire

Valider V55 sur PostgreSQL réel ou Testcontainers, puis finaliser le changelog et le suivi avant fusion.
