# EPIC-0026 — Cloisonnement permission-first de tous les rôles

## Objectif

Garantir qu'aucun rôle système ou personnalisé n'accède à un menu, une route ou une API sans la permission effective correspondante. Le backend reste l'autorité de sécurité ; le frontend applique la même matrice pour éviter les interfaces trompeuses.

## Contexte

L'incident `BUG-20260718-PATIENT-PROFESSIONAL-RBAC-CONTEXT-LEAK` a révélé un cache RBAC non lié au jeton et des fallbacks `rôle OU permission`. Le correctif P0 neutralise la contamination entre toutes les sessions et verrouille les disponibilités. L'inventaire initial identifie 21 contrôleurs `@PreAuthorize` encore exclusivement fondés sur des rôles, en plus de routes Angular sans permission déclarée.

## Périmètre

### Inclus

- matrice rôle → permissions → menus → routes → endpoints ;
- migration permission-first par domaine ;
- rôles système et personnalisés ;
- tests positifs et négatifs croisés ;
- isolation tenant et objet métier en complément de l'autorisation fonctionnelle ;
- portail patient avec permissions/capacités explicites ou frontière de rôle documentée.

### Exclus

- refonte de l'authentification, de l'OTP ou du stockage des refresh tokens ;
- changement des règles métier propres à chaque domaine ;
- création massive de permissions sans validation des responsables métiers.

## Découpage

| ID | User story | Objectif | SP | Profil | Reviewer | Statut |
|---|---|---|---:|---|---|---|
| STORY-2701 | Matrice d'autorisation canonique | Recenser chaque action et obtenir validation métier/RSSI | 3 | Architecte sécurité + métiers | RSSI + Product Owner | QA MÉTIER |
| STORY-2702 | Navigation et routes Angular | Supprimer les fallbacks rôle sur les actions permissionnées et tester toutes les permutations | 5 | Senior Angular | Lead Frontend + QA sécurité | DONE TECHNIQUE |
| STORY-2703 | APIs cliniques et patient | Migrer consultations, visites, documents, urgences et patient sur authorities + contrôles objet | 5 | Senior backend santé | Lead Backend + médecin référent | DONE TECHNIQUE |
| STORY-2704 | APIs administratives et financières | Migrer staff, clinique, facturation, caisse, audit et stock sur authorities | 5 | Senior backend | Lead Backend + DAF + RSSI | DONE TECHNIQUE |
| STORY-2705 | Tests matriciels et observabilité | Ajouter tests E2E multi-rôles, audit des refus et gate CI | 3 | QA automation sécurité | Tech Lead + RSSI | QA |

Total : **21 SP**, environ **8 à 10 jours senior**, à répartir sur **deux sprints** avec 20 % de marge sécurité.

## Critères d'acceptation

- [x] Chaque action sensible possède une permission canonique dans la matrice technique.
- [x] Toute route permissionnée exige cette permission sans fallback implicite sur le rôle.
- [x] Tout endpoint sensible exige l'authority correspondante et conserve les contrôles tenant/propriétaire.
- [x] Un changement de compte ne conserve jamais les permissions de la session précédente.
- [x] Les rôles système, les rôles personnalisés et les refus voisins sont couverts par les tests de politique et d'intégration.
- [x] Les claims mixtes avec `PATIENT` restent strictement patient.
- [x] La suite Maven contient un test de politique bloquant la réintroduction de `hasRole` dans les contrôleurs.

## Definition of Ready

- [x] Incident et causes documentés.
- [x] Inventaire initial frontend/backend disponible.
- [x] ADR permission-first proposé.
- [ ] Matrice approuvée par Product Owner, RSSI, DAF et référents cliniques.
- [ ] Capacité de deux sprints réservée.

## Definition of Done

- [x] Code, contrats et documentation synchronisés.
- [x] Suites Maven et Angular vertes.
- [ ] Recette E2E multi-rôles humaine signée.
- [ ] Revue Tech Lead et RSSI approuvée.
- [ ] Recette croisée de tous les rôles signée.
- [x] Aucun fallback professionnel legacy restant dans les contrôleurs, routes, menus ou actions audités.

## Risques et dépendances

- Une conversion mécanique pourrait retirer un accès métier légitime : validation par domaine obligatoire.
- Les 21 contrôleurs rôle-only ne sont pas tous vulnérables ; ils représentent une dette de cohérence, pas une preuve d'exploitation.
- Dépendances : catalogue `RbacCatalog`, rôles personnalisés, propriétaires métiers, CI et disponibilité du cache Maven.

## Impact version / SemVer

- Correctif P0 immédiat : **PATCH**.
- EPIC-0026 complet : **MINOR** si de nouvelles permissions publiques sont introduites ; sinon PATCHs progressifs.
