# BUG-20260710-V55-H2-COMPATIBILITY — Migration V55 incompatible avec H2

## Mode

Diagnostic + Engineering — correction de migration Flyway rétrocompatible.

**Statut :** IN_PROGRESS — cause confirmée, correction et validations en cours.

## Contexte

La migration `V55__billing_amounts_float_to_numeric.sql` aligne les colonnes financières avec le passage Java de `Double` vers `BigDecimal`.

La production cible PostgreSQL, tandis que les tests backend et la CI exécutent Flyway sur H2. La migration utilise actuellement des constructions propres à PostgreSQL :

- cast `::NUMERIC` ;
- clause `USING` dans `ALTER COLUMN` ;
- regroupement de plusieurs changements de type dans une seule instruction.

Cette syntaxe bloque le démarrage du contexte de test sur H2 et empêche la validation backend complète.

## Objectif

Rendre V55 exécutable sur PostgreSQL et H2 sans modifier la sémantique financière, les contrats API, les valeurs existantes ou la précision cible.

## Périmètre inclus

- migration Flyway V55 ;
- documentation fonctionnelle et technique de la migration ;
- vérification de la conversion des colonnes financières ;
- tests backend et CI.

## Périmètre exclu

- changement des règles de calcul financier ;
- modification des DTO ou entités BigDecimal déjà migrés ;
- refonte du moteur de tests vers Testcontainers ;
- modification des statuts de facture ou de paiement.

## Critères d’acceptation

- [ ] V55 ne contient plus de cast PostgreSQL `::`.
- [ ] V55 ne dépend plus de la clause PostgreSQL `USING`.
- [ ] Les types finaux restent `NUMERIC(19,4)` pour les montants et `NUMERIC(5,4)` pour le taux de couverture.
- [ ] Les valeurs nulles historiquement normalisées conservent le même comportement : remise à `0` et coefficient à `1`.
- [ ] Les migrations Flyway s’exécutent sur H2 utilisé par les tests.
- [ ] La syntaxe reste valide sur PostgreSQL selon la documentation officielle.
- [ ] `./mvnw test` réussit.
- [ ] `./mvnw clean verify -Dspring.profiles.active=test` réussit ou tout blocage indépendant est documenté.
- [ ] Aucun contrat API, calcul métier ou permission n’est modifié.

## Plan d’action

- [x] Lire les règles IA et les standards du dépôt.
- [x] Confirmer la configuration H2 des tests et de la CI.
- [x] Identifier les constructions SQL incompatibles.
- [x] Vérifier la précision attendue dans les entités JPA.
- [ ] Documenter la stratégie fonctionnelle et technique.
- [ ] Remplacer les conversions spécifiques PostgreSQL par des opérations SQL portables et atomiques.
- [ ] Vérifier le diff pour exclure tout changement métier.
- [ ] Exécuter les tests backend via la CI de la Pull Request.
- [ ] Mettre à jour le changelog et le suivi projet.
- [ ] Ouvrir une Pull Request dédiée.

## Definition of Ready

- [x] Cause technique identifiée.
- [x] Périmètre limité à V55 et à sa documentation.
- [x] Critères d’acceptation définis.
- [x] Risques de données identifiés.

## Definition of Done

- [ ] Migration portable appliquée.
- [ ] Tests backend verts.
- [ ] Documentation, ticket, changelog et suivi à jour.
- [ ] Revue du risque PostgreSQL/H2 réalisée.
- [ ] Pull Request ouverte et prête pour review.

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

## Sécurité et régression

- Aucun endpoint, rôle ou contrôle multi-tenant n’est modifié.
- Risque principal : arrondi ou perte de valeur durant la conversion FLOAT8 vers NUMERIC.
- Mesure : conserver les précisions existantes et ne modifier que la syntaxe de conversion.
- Risque secondaire : changement involontaire des valeurs nulles.
- Mesure : normaliser explicitement les seules colonnes déjà couvertes par `COALESCE` dans la migration actuelle.

## Impact version

- **PATCH** : correction rétrocompatible d’une migration non fonctionnelle dans l’environnement de test.

## Références

- PostgreSQL `ALTER TABLE ... ALTER COLUMN ... SET DATA TYPE` : en l’absence de `USING`, PostgreSQL applique la conversion d’affectation disponible.
- Configuration H2 : `backend/src/test/resources/application-test.yml`.
- CI backend : `.github/workflows/ci.yml`.

## Reste à faire

Appliquer la syntaxe portable, exécuter la CI puis clôturer le ticket avec les preuves de test.