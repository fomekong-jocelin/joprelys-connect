# Conception Technique — Démarrage du Backend Joprelys Connect

## 1. Architecture & Composants
- **Stack** : Java 21 LTS, Spring Boot 4.0.0-M2 / Maven 3.9, PostgreSQL 17, Flyway.
- **Port d'écoute** : Port standard Spring Boot 8080 (HTTP).
- **Base de données** : PostgreSQL local (`postgresql-x64-17`), écoute sur le port 5432, base de données cible `joprelys`, utilisateur `postgres`, mot de passe `postgres`.
- **Mécanisme de migration** : Flyway est activé (`validate-on-migrate: false`, `repair-on-migrate: true`) au démarrage via `application.yml`. Schéma migré jusqu'à V110.

## 2. Configuration & Profils
- Configuration principale : `backend/src/main/resources/application.yml`.
- Variables d'environnement utilisées par défaut :
  - `SPRING_DATASOURCE_URL=jdbc:postgresql://localhost:5432/joprelys`
  - `SPRING_DATASOURCE_USERNAME=postgres`
  - `SPRING_DATASOURCE_PASSWORD=postgres`
  - `JOPRELYS_AI_ENABLED=false` (mode standard par défaut pour démarrage rapide local sans dépendance externe bloquante)
  - `JOPRELYS_MEDICATION_REFERENCE_ENABLED=false`
  - `JOPRELYS_RXNORM_ENABLED=false`

## 3. Démarche d'exécution & Processus
1. Validation du service PostgreSQL local (`postgresql-x64-17` actif sur le port 5432).
2. Contrôle de l'existence de la base `joprelys` et des identifiants valides.
3. Sauvegarde préventive `scratch/joprelys_backup_pre_v88.sql`.
4. Nettoyage des tables spatiales obsolètes bloquant la migration V88.
5. Correction du conditionnement Spring context pour `FinalClinicalReviewController`, `FinalClinicalReviewService`, et `OpenAiFinalClinicalReviewGateway` afin de respecter `joprelys.ai.enabled=false`.
6. Compilation des sources (`.\mvnw.cmd compile`).
7. Lancement en arrière-plan daemon avec commande Maven Spring Boot (`.\mvnw.cmd spring-boot:run`).
8. Surveillance des logs de démarrage de Spring Boot (`Started JoprelysBackendApplication in 10.105 seconds`).
9. Vérification HTTP sur `http://localhost:8080/v3/api-docs` (HTTP 200) et `http://localhost:8080/swagger-ui/index.html` (HTTP 200).

## 4. Tests & Vérifications
- Test de connectivité base de données : TCP 5432 + requête SQL psql -> Succès.
- Compilation Maven : Code de sortie 0.
- Migrations Flyway : 23 migrations appliquées (jusqu'à V110).
- Test HTTP du serveur démarré : Réponse HTTP 200 OK sur `/v3/api-docs` et `/swagger-ui/index.html`.
