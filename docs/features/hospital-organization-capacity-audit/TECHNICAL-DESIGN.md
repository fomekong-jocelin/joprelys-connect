# Conception technique initiale — Audit organisation hospitalière

## Nature du travail

Cette phase ne modifie pas le comportement applicatif. Elle reconstitue l'existant puis définit une architecture cible proposée et un plan de migration à valider.

## Sources auditées

- migrations Flyway et schéma déductible ;
- entités, repositories, services/use cases et contrôleurs Spring Boot ;
- DTO, statuts, validations, sécurité et audit ;
- routes, services API, modèles, composants et i18n Angular ;
- tests backend/frontend et fixtures accessibles ;
- tickets, spécifications, ADR, cahiers des charges et suivi projet.

## Méthode de preuve

1. Le code de `main` et les migrations priment sur les déclarations historiques.
2. Une fonction est `existante` si elle possède une implémentation utilisable et un point d'accès cohérent.
3. Elle est `partielle` si seule une couche ou un sous-parcours est présent.
4. Elle est `défectueuse` si le modèle ou le flux contredit une règle critique ou produit un risque prouvé.
5. Elle est `absente` si aucun artefact DB/API/UI cohérent n'est retrouvé.

## Contrôles techniques prioritaires

- cardinalités et rattachements multiples ;
- contraintes temporelles et de non-chevauchement ;
- cohérence lit/occupation/hospitalisation ;
- verrouillage et transactions ;
- soft delete, dates d'effet et audit ;
- isolation tenant et permissions fines ;
- responsabilités backend/frontend ;
- capacité de reporting et interopérabilité.

## Sorties techniques attendues

- cartographie actuelle ;
- modèle de données cible et règles d'historisation ;
- workflows et machines à états ;
- exigences API, sécurité, observabilité et migration ;
- backlog découpé, estimé et testable ;
- ADR au statut `Proposed`, sans décision d'implémentation implicite.

## Impact SemVer anticipé

- audit seul : aucun bump ;
- nouveaux concepts ajoutés de manière compatible : MINOR possible ;
- remplacement de `wards/rooms/beds`, contrats obligatoires ou migrations incompatibles : MAJOR probable, avec migration progressive recommandée.

## Constats techniques finaux

- `bed_assignments.hospitalization_id` n'est pas une FK et aucune contrainte n'impose une seule période active par lit.
- Le claim atomique `FREE → OCCUPIED` est une bonne protection locale, mais ne remplace pas l'intégrité temporelle SQL.
- L'endpoint de statut lit autorise des transitions désynchronisant lit, affectation et séjour.
- Les noms service/chambre/lit sont dupliqués dans le séjour et le service de visite reste textuel.
- Les entités de personnel ne portent aucune affectation métier datée.
- Les contraintes de production ne doivent plus être omises pour satisfaire H2 ; utiliser PostgreSQL/Testcontainers pour les invariants.
- Les services de configuration spatiale et d'hospitalisation approchent la limite de taille et doivent être découpés par use case lors de l'implémentation.

## Architecture cible proposée

La proposition détaillée se trouve dans [DATA-MODEL.md](DATA-MODEL.md) et [ADR-0002](../../architecture/adr/ADR-0002-flexible-hospital-organization-and-capacity-model.md). Elle utilise des agrégats séparés, des événements append-only, des projections de capacité et des commandes idempotentes. Aucun controller ne doit manipuler repository ou statut critique directement.

## Stratégie de livraison

1. ajouter contraintes et garde-fous sans changer les contrats publics ;
2. créer les référentiels cibles et API v2 en parallèle ;
3. migrer et réconcilier les données ;
4. basculer les écrans par feature flag ;
5. déprécier puis retirer le legacy lors d'une version breaking.

## Vérifications réalisées pendant l'audit

- lecture croisée migrations/entités/repositories/services/controllers ;
- inventaire des routes, formulaires, menus, i18n et permissions Angular ;
- inspection des tests de claim/transfert concurrent et des parcours urgences/hospitalisation ;
- vérification Maven, YAML Spring, proxy Angular et `.gitignore` ;
- absence confirmée des entités site/bâtiment/étage/unité/équipement/imagerie.
