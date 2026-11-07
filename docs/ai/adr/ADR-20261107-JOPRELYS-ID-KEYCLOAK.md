# ADR-20261107 — Joprelys ID sur Keycloak

## Statut

Accepté (choix de Keycloak tranché par le porteur produit le 07/11/2026, point 4 du § 4 de la feuille de route de l'écosystème)

## Date

2026-11-07

## Contexte

L'écosystème santé compte trois applications autonomes : AllôPharma (patient et famille), Dictalys (médecin) et Joprelys (HIS, cœur du DPU). La règle « standalone-first, écosystème-ready » impose que **chaque application garde sa propre connexion**. Joprelys ID ne sert qu'à **relier deux applications** :

- appli → appli au nom d'un utilisateur : OAuth2 *authorization code* + PKCE, avec écran de consentement ;
- serveur → serveur : *client credentials*, avec des scopes par contrat.

Le premier flux à ouvrir est la liaison patient ↔ DPU (AllôPharma → Joprelys), en environnement de test.

Joprelys possède déjà une authentification maison (JWT HS, sessions persistantes, OTP patient du portail, RBAC « permission-first »). Elle reste inchangée : Joprelys ID ne la remplace pas.

Deux options étaient sur la table :

1. un serveur d'autorisation embarqué dans le backend Joprelys (Spring Authorization Server) ;
2. un fournisseur d'identité dédié, Keycloak.

## Décision

Joprelys ID est un **realm Keycloak dédié** (`joprelys-id`), déployé à côté des applications, et non dans le backend Joprelys.

- Version : Keycloak **26.x** (tag d'image exact épinglé dans `docker-compose` et dans les tests ; pas de `latest`).
- Clients du realm :
  - `allopharma-mobile` : client **public**, *authorization code* + **PKCE S256 obligatoire**, *consent required*, redirections limitées au schéma de l'app (`cm.allopharma.app:/oauth2redirect`), pas de *direct access grant*, pas d'*implicit flow* ;
  - `allopharma-server` : client **confidentiel**, *client credentials* seulement (compte de service), secret fourni par variable d'environnement, jamais versionné ;
  - `joprelys-api` : ressource visée (audience) des jetons ; aucun flux de connexion.
- Scopes de contrat (client scopes) : `eco:patient-link` (créer une liaison au nom du patient), `eco:consent.revoke` (révoquer depuis un serveur), puis les scopes de lecture à venir (`eco:prescriptions`, `eco:lab-results`…).
- Jetons courts (5 min), pas de *refresh token* stocké côté serveur AllôPharma : le jeton patient ne sert qu'à créer la liaison, puis il est jeté.
- Le DPU de la personne est porté par l'attribut utilisateur `dpu`, exposé par un *protocol mapper* dans le jeton. En test, ces attributs sont fictifs (`DPU-TEST-…`). La preuve d'identité qui lie un compte Joprelys ID à un vrai DPU (OTP vérifié par Joprelys) fait l'objet d'une story ultérieure.
- Le backend Joprelys valide les jetons Joprelys ID comme **resource server** (`spring-boot-starter-oauth2-resource-server`) sur une chaîne de filtres **dédiée à `/api/eco/v1/**`**. Le reste de l'API garde l'authentification existante.
- Les webhooks serveur → serveur ne passent pas par Keycloak : ils sont signés par HMAC (`Eco-Signature`, § 2.2 de la feuille de route).

## Raisons

- Produit mature et maintenu : consentement OAuth, PKCE, comptes de service, rotation des clés de signature et journal d'événements fournis sans code maison.
- Séparation des responsabilités : le fournisseur d'identité de liaison ne dépend pas du cycle de livraison du HIS. Une panne ou une mise à jour de Joprelys n'empêche pas Dictalys et AllôPharma de se relier, et inversement.
- Standalone-first respecté : aucune appli n'a besoin de Keycloak pour fonctionner seule ; il n'intervient que pour relier.
- Testable localement (image Docker, import de realm JSON, module Testcontainers).
- Évolutif : fédération d'identité, MFA, thème du consentement en français, sans toucher aux applis.

## Conséquences positives

- Les clés API Joprelys inopérantes (P1-5) seront remplacées par des comptes de service *client credentials*.
- Contrôle d'accès par scope et par audience, vérifié côté serveur Joprelys.
- Le consentement OAuth est affiché par le fournisseur d'identité, hors de l'app cliente.

## Conséquences négatives / risques

- Un composant de plus à exploiter (base PostgreSQL dédiée, sauvegardes, mises à jour de sécurité, supervision) : à chiffrer côté VPS, hors de cette ADR.
- Empreinte mémoire (~512 Mo à 1 Go) sur le VPS.
- Thème de la page de connexion et de consentement à réaliser pour rester cohérent avec les chartes (FR/EN, WCAG 2.2 AA).
- Risque de confusion avec l'authentification maison de Joprelys : la chaîne de filtres `/api/eco/v1/**` doit refuser les jetons maison, et le reste de l'API doit refuser les jetons Joprelys ID (tests dédiés).
- La liaison compte Joprelys ID ↔ DPU réel n'est pas résolue par cette ADR (story ultérieure de preuve d'identité).

## Alternatives rejetées

| Alternative | Raison du rejet |
|---|---|
| Spring Authorization Server dans le backend Joprelys | Couple le fournisseur d'identité de l'écosystème au cycle de livraison du HIS ; consentement, comptes de service et rotation de clés à écrire et maintenir nous-mêmes |
| Fournisseur SaaS (Auth0, Okta, Cognito) | Coût récurrent, données d'identité santé hébergées hors de notre contrôle |
| Réutiliser les JWT maison Joprelys pour les autres applis | Pas de consentement, pas de PKCE, secret partagé HS256 : incompatible avec des clients tiers |
| Une connexion unique (SSO) pour les trois applis | Contraire à la règle standalone-first : chaque appli garde sa propre connexion |

## Impact planning

| Élément | Impact |
|---|---|
| Charge | Story S1 (3 SP) pour l'environnement de dev ; resource server Joprelys inclus dans S3 |
| Risque | Moyen : nouveau composant, mais périmètre limité au test |
| Profils nécessaires | Senior back-end / sécurité |
| Sprint impacté | S3-S4 de la feuille de route (EPIC ECO-20261107) |

## Références

- `C:\MES-APPLICATIONS\PharmaLocate\feuille-de-route-ecosysteme.md`, § 0, § 2.1, § 2.2, § 2.3, § 4
- Ticket EPIC `docs/ai/tickets/ECO-20261107-JOPRELYS-ID-PATIENT-LINK.md`
- ADR-20260718-PERMISSION-FIRST-AUTHORIZATION (authentification maison inchangée)
- Documentation Keycloak 26 (Server Administration Guide : clients, client scopes, consent, service accounts ; Server Container Guide : import de realm)
- Spring Security : OAuth 2.0 Resource Server JWT
- RFC 7636 (PKCE), RFC 8252 (OAuth pour les applications natives)
