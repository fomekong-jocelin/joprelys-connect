# TICKET-0112 — Complétion du Module 1 (Gestion des établissements)

## 1. Objectif

Compléter la gestion des établissements (Module 1) dans Joprelys Connect, tant sur le plan du modèle de données de base de données, des API backend Spring Boot que de l'IHM frontend Angular.
L'objectif est d'aligner le module avec les exigences réglementaires et de sécurité du Cahier des Charges.
- Ajouter les colonnes manquantes dans la table `organizations` : `type` (type d'acteur), `country` (pays), `responsible_name` (nom du responsable) et `api_enabled` (autorisation API).
- Mettre en place un système dynamique de génération et de révocation des clés API en base de données par organisation.
- Configurer les restrictions d'accès M2M par clé API en fonction du statut de l'organisation et de l'activation de son accès API.
- Adapter les formulaires et listes de l'IHM Angular d'administration pour gérer ces nouveaux champs et permettre la gestion des clés API.

## 2. Critères d'acceptation

- [x] Migration de base de données Flyway `V28` écrite et validée.
- [x] Entité `OrganizationEntity` et les DTOs backend enrichis avec `type`, `country`, `responsible_name`, `api_enabled`.
- [x] Table `organization_api_keys` créée avec gestion d'empreinte sécurisée (SHA-256) des clés.
- [x] Endpoints de génération et révocation des clés API implémentés sur `/api/organizations/{id}/api-keys` réservés au rôle `ADMIN_JOPRELYS`.
- [x] Intercepteur/Filtre de sécurité Spring Boot (`ApiKeyAuthenticationFilter`) validant la clé passée en header HTTP `X-API-KEY`.
- [x] Rejet des appels API (403 Forbidden) pour tout établissement désactivé/suspendu ou pour lequel `api_enabled` est faux.
- [x] Formulaires Angular mis à jour avec les nouveaux champs obligatoires (Type, Pays, Nom du responsable).
- [x] Interface utilisateur mise à jour pour afficher et gérer les clés API de chaque organisation.
- [x] Tests d'intégration et unitaires backend/frontend écrits et passant au vert.
- [x] Spécification fonctionnelle et technique rédigée sous `./docs/features/organizations/`.

## 3. Pilotage projet

| Champ | Valeur |
|---|---|
| Epic parent | EPIC-0002 — Gestion des Établissements |
| User story parent | STORY-0201 — Administration des établissements et clés API |
| Sprint cible | SPRINT-0010 (ou SPRINT-0009 si priorisé immédiatement) |
| Priorité business | P1 |
| Complexité | M |
| Story points | 3 |
| Profil recommandé | Senior |
| Effort estimé senior | 0.4j |
| Effort estimé intermédiaire | 0.6j |
| Effort estimé junior | 1.0j |
| Responsable | Antigravity |
| Reviewer obligatoire | Lead Developer / Jocelin |
| Risque fonctionnel | Moyen |
| Risque technique | Moyen |
| Dépendances | Aucun |
| Bloquants connus | Aucun |

## 4. Contexte analysé

- [x] `Cahier_des_charges_Joprelys_Connect_Complet.md` lu (Module 1, FR-ORG)
- [x] Code existant backend (`OrganizationController`, `OrganizationEntity`, etc.) analysé
- [x] Code existant frontend (`organization-list.component`, `organizations.models.ts`, etc.) analysé
- [x] Rapport d'audit `audit_rapport_complet.md` lu

## 5. Hypothèses

- Le type d'établissement doit respecter une liste d'énumérations restrictives : HOSPITAL, CLINIC, CABINET, LABORATORY, IMAGING_CENTER, PHARMACY, HEALTH_PLATFORM, NGO, INSTITUTION.
- La clé API générée est visible une seule fois par l'utilisateur (lors de la création) et stockée sous forme hachée sécurisée (SHA-256) en base de données.
- L'entête standard utilisé pour s'authentifier par clé API est `X-API-KEY`.

## 6. Risques et impacts

| Risque | Impact | Mitigation |
|---|---|---|
| Fuite de clé API | Compromission des données patients de l'établissement | Stocker uniquement le hash en base de données. Forcer l'affichage de la clé en clair une seule fois à la génération. |
| Incompatibilité de schéma lors de la migration | Échec du démarrage de l'application | Écrire une migration Flyway rétrocompatible et inclure des valeurs par défaut pour les organisations existantes. |

## 7. Action plan

- **Étape 1 : Conception & Spécifications**
  - Rédiger `docs/features/organizations/FUNCTIONAL-SPEC.md` et `TECHNICAL-DESIGN.md`.
- **Étape 2 : Database & Modèle Backend**
  - Créer la migration Flyway `V28__create_organization_extensions_and_api_keys.sql`.
  - Mettre à jour `OrganizationEntity.java`.
  - Créer l'entité `OrganizationApiKeyEntity.java` et son repository.
- **Étape 3 : Endpoints & Services Backend**
  - Mettre à jour les DTOs `CreateOrganizationRequest`, `UpdateOrganizationRequest` et `OrganizationResponse`.
  - Ajouter la gestion des clés API dans un nouveau service ou controller.
  - Implémenter le filtre de sécurité `ApiKeyAuthenticationFilter`.
- **Étape 4 : Frontend Angular**
  - Mettre à jour `organizations.models.ts` et `organization-api.service.ts`.
  - Mettre à jour le formulaire de création/édition d'organisation.
  - Ajouter le composant/panneau de gestion des clés API.
- **Étape 5 : Tests & Validation**
  - Écrire les tests unitaires et MockMvc backend.
  - Écrire les tests frontend Angular.

## 8. Implémentation réalisée

- Migration Flyway `V28` écrite pour ajouter les colonnes `type`, `country`, `responsible_name`, `api_enabled` à la table `organizations` et créer la table `organization_api_keys`.
- JPA entities (`OrganizationEntity`, `OrganizationApiKeyEntity`), repositories et DTOs mis à jour/créés.
- `OrganizationController` enrichi pour gérer les clés API (génération de clé random `jop_live_...`, hashage SHA-256 pour persistance DB, liste avec masquage, révocation).
- `ApiKeyAuthenticationFilter` configuré dans `SecurityConfig` pour intercepter `X-API-KEY` (sauf sur les routes publiques `/api/public/` qui valident leurs propres clés). Le filtre vérifie le statut de l'organisation et injecte le rôle `ROLE_API_CLIENT` tout en positionnant le `TenantContext`.
- Modèles, formulaire de création, drawer d'édition et détails mis à jour côté Angular (intégration des nouveaux champs et de l'IHM de gestion des clés API).
- Les tests unitaires/intégration backend (228/228 passant) et frontend (63/63 passant) valident le bon fonctionnement.

## 9. Suivi d'exécution

| Date | Développeur | Temps passé | Avancement | Reste à faire | Blocage | Commentaire |
|---|---|---:|---:|---:|---|---|
| 2026-07-05 | Antigravity | 0.05j | 0% | Conception et découpage | Aucun | Ticket initialisé et mis au statut READY |
| 2026-07-05 | Antigravity | 0.35j | 100% | Aucun | Aucun | Implémentation complète backend/frontend et tests OK |

## 10. Tests et vérifications

- Tests unitaires et d'intégration backend passés avec succès via `./mvnw test` (228 tests au total, dont les nouveaux scénarios d'API Key).
- Tests unitaires frontend passés avec succès via `npm run test -- --watch=false` (63 tests validés).
- Build de production frontend validé via `npm run build`.

## 11. Documentation

- [x] Spécification fonctionnelle et technique rédigée
- [x] Changelog mis à jour
- [x] Suivi projet mis à jour

## 12. Reste à faire

- Aucun. Tout est complété.

## 13. Statut final

Statut : DONE
