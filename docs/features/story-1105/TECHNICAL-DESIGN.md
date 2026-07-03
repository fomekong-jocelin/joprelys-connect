# TECHNICAL-DESIGN — STORY-1105 : Nettoyage de la dette technique & QA

## 1. Scope technique

Correction de la configuration Maven et Spring Boot pour garantir la résolvabilité des dépendances de test et la stabilité des tests d'intégration sous H2.

## 2. Corrections pom.xml

### Dépendances supprimées (inexistantes dans Maven Central)

```xml
<!-- SUPPRIMÉES — Ces artefacts n'existent pas -->
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-data-jpa-test</artifactId>
    <scope>test</scope>
</dependency>
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-flyway-test</artifactId>
    <scope>test</scope>
</dependency>
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-security-test</artifactId>
    <scope>test</scope>
</dependency>
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-validation-test</artifactId>
    <scope>test</scope>
</dependency>
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-webmvc-test</artifactId>
    <scope>test</scope>
</dependency>
```

### Dépendance ajoutée (correcte)

```xml
<!-- AJOUTÉE — Artefact officiel Spring Security pour les tests MockMvc -->
<dependency>
    <groupId>org.springframework.security</groupId>
    <artifactId>spring-security-test</artifactId>
    <scope>test</scope>
</dependency>
```

> **Note :** `spring-boot-starter-test` (déjà présent) fournit : JUnit 5, Mockito, MockMvc, AssertJ, Hamcrest, JSONPath et `@SpringBootTest`. `spring-security-test` fournit `@WithMockUser` et `SecurityMockMvcRequestPostProcessors`.

## 3. Corrections application.yml (main)

```yaml
spring:
  flyway:
    enabled: true
    validate-on-migrate: false
    repair-on-migrate: true   # ← AJOUTÉ
```

La propriété `repair-on-migrate: true` déclenche automatiquement `flyway repair` avant chaque migration. Cela corrige les checksums corrompus sans intervention manuelle en cas de modification d'un script déjà appliqué (scénario fréquent en développement local).

## 4. Profil application-test.yml

### Décisions d'architecture

| Choix | Justification |
|---|---|
| `H2 MODE=PostgreSQL` | Compatibilité maximale avec les types et fonctions PostgreSQL |
| `DB_CLOSE_DELAY=-1` | La base H2 reste ouverte pendant toute la durée des tests |
| `DB_CLOSE_ON_EXIT=FALSE` | Évite la fermeture prématurée lors des tests parallèles |
| `ddl-auto: none` | Flyway gère entièrement le schéma, aucune génération automatique Hibernate |
| `validate-on-migrate: false` | Les migrations H2 peuvent avoir de légères différences de type |
| `repair-on-migrate: true` | Cohérence avec le profil principal |
| `seed.admin.enabled: false` | Évite la création d'utilisateurs parasites pendant les tests |
| `storage-dir: ./target/test-storage` | Utilise le répertoire Maven `target` nettoyé à chaque build |

### Configuration complète

```yaml
spring:
  datasource:
    url: jdbc:h2:mem:testdb;DB_CLOSE_DELAY=-1;MODE=PostgreSQL;DB_CLOSE_ON_EXIT=FALSE
    username: sa
    password:
    driver-class-name: org.h2.Driver
  jpa:
    hibernate:
      ddl-auto: none
    open-in-view: false
  flyway:
    enabled: true
    validate-on-migrate: false
    repair-on-migrate: true
    locations: classpath:db/migration

joprelys:
  security:
    jwt:
      issuer: joprelys-test
      audience: joprelys-test
      secret: test-jwt-secret-at-least-32-chars-long-hs256
      ttl-minutes: 30
  seed:
    admin:
      enabled: false
  documents:
    storage-dir: ./target/test-storage
    verification-base-url: http://localhost/verify
  lab-integration:
    api-key: test-api-key
```

## 5. Vérification .gitignore

Le fichier `.gitignore` racine a été vérifié — il contient déjà :
- `target/` (ligne 51) — exclusion des builds Maven ✅
- `.env` (ligne 35) — secrets non versionnés ✅
- `*.log` (ligne 25) — logs ignorés ✅

Aucune modification nécessaire.

## 6. Tests impactés

Tous les tests `@SpringBootTest` annotés avec `@ActiveProfiles("test")` bénéficient automatiquement de cette configuration. Les tests MockMvc utilisant `@WithMockUser` peuvent maintenant importer correctement `spring-security-test`.

## 7. Commande de vérification

```bash
cd backend
mvn dependency:resolve -q
mvn test -Dspring.profiles.active=test
```
