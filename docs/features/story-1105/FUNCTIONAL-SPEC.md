# FUNCTIONAL-SPEC — STORY-1105 : Nettoyage de la dette technique & QA

## 1. Contexte fonctionnel

Ce ticket de nettoyage résout des problèmes de configuration Maven et Spring Boot qui empêcheraient la compilation et l'exécution des tests unitaires en CI/CD. Il n'apporte aucune nouvelle fonctionnalité mais sécurise la chaîne de build.

## 2. Problèmes identifiés

### 2.1 Dépendances Maven inexistantes (BLOQUANT BUILD)

Les artefacts suivants ont été déclarés dans `pom.xml` mais n'existent pas dans Maven Central pour Spring Boot 4.1 :

| Artefact invalide | Raison |
|---|---|
| `spring-boot-starter-data-jpa-test` | N'existe pas — utiliser `spring-boot-starter-test` |
| `spring-boot-starter-flyway-test` | N'existe pas — Flyway est testé via H2 + `spring-boot-starter-test` |
| `spring-boot-starter-security-test` | N'existe pas — utiliser `spring-security-test` de `org.springframework.security` |
| `spring-boot-starter-validation-test` | N'existe pas — validation testée via MockMvc |
| `spring-boot-starter-webmvc-test` | N'existe pas — MockMvc fourni par `spring-boot-starter-test` |

### 2.2 Configuration Flyway incomplète

`repair-on-migrate: true` absent de `application.yml` principal, empêchant la réparation automatique des checksums Flyway en cas de script modifié localement.

### 2.3 Profile de test incomplet

Le fichier `src/test/resources/application.yml` existant ne définissait pas :
- L'isolation complète via `DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE`
- Le secret JWT test conforme (32+ caractères)
- La désactivation du seed admin (`seed.admin.enabled: false`)
- Les propriétés `documents` et `lab-integration` nécessaires aux tests d'intégration

## 3. Corrections apportées

| Fichier | Action |
|---|---|
| `backend/pom.xml` | Suppression de 5 dépendances test invalides, ajout de `spring-security-test` |
| `src/main/resources/application.yml` | Ajout de `repair-on-migrate: true` sous `flyway:` |
| `src/test/resources/application-test.yml` | Création du profil de test complet avec H2, JWT, Flyway et propriétés métier |

## 4. Impact utilisateur

Aucun impact fonctionnel pour les utilisateurs finaux. Impact direct sur la stabilité du build Maven et l'exécution des tests automatisés.

## 5. Critères d'acceptation

- [x] `mvn dependency:resolve` ne remonte plus d'artefacts introuvables
- [x] `mvn test` s'exécute sans erreur de résolution de dépendances
- [x] Le profil `test` dispose de toutes les propriétés requises par les tests d'intégration existants
- [x] `repair-on-migrate: true` actif en développement et en test
