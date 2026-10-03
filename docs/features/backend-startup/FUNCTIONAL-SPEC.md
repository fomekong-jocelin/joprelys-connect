# Spécification Fonctionnelle — Démarrage du Backend Joprelys Connect

## 1. Contexte & Problématique
Le projet Joprelys Connect dispose d'un backend Spring Boot centralisant l'ensemble des règles métier de santé (dossiers patients, consultations, admissions, urgences, pharmacie, facturation, IA clinique).
Pour permettre le travail local des développeurs, l'exécution des tests d'intégration, ou l'utilisation par les interfaces Web Angular et Mobile Flutter, le backend doit pouvoir être démarré localement et être joignable de manière stable et sécurisée.

## 2. Utilisateurs & Périmètre
- **Acteurs** : Développeurs, testeurs, équipes Frontend Web & Mobile, services automatisés.
- **Périmètre inclus** :
  - Vérification des prérequis d'infrastructure locale (service PostgreSQL 17, base `joprelys`).
  - Validation de la configuration d'accès à la base de données et des propriétés Spring Boot.
  - Exécution des migrations Flyway jusqu'à la dernière version disponible.
  - Démarrage effectif du serveur Spring Boot (embedded Tomcat).
  - Validation du point d'accès API (Swagger/OpenAPI `/v3/api-docs` ou port HTTP 8080).
- **Périmètre exclu** :
  - Déploiement distant sur les environnements de recette/production (géré par scripts dédiés).
  - Modification non concertée de la logique métier applicative.

## 3. Critères d'acceptation
- **AC-1** : L'instance PostgreSQL locale est active et accessible avec les identifiants configurés (`postgres`/`postgres` sur le port 5432).
- **AC-2** : La base de données `joprelys` existe et les tables/migrations nécessaires sont exécutées ou validées sans erreur.
- **AC-3** : Le projet Spring Boot compile sans erreur (`mvnw compile`).
- **AC-4** : Le backend démarre via `mvnw spring-boot:run` ou exécution du jar/classe principale et écoute sur le port configuré (port par défaut 8080).
- **AC-5** : Une requête HTTP sur l'API (ex: documentation Swagger `/swagger-ui/index.html` ou `/v3/api-docs`) répond avec succès.
