# TICKET-0016-API-MODULE — Module 16 API Joprelys Connect (Sous-tâche 1 & 2)

## 1. Objectif

Aligner le module API avec le Cahier des Charges Module 16 (Principes API), sans créer de doublons avec l'existant.

## 2. Critères d'acceptation

- [ ] `GlobalExceptionHandler` créé avec format d'erreur CDC : `{ "error": { "code", "message", "trace_id" } }`
- [ ] `trace_id` généré pour chaque requête et inclus dans les réponses d'erreur
- [ ] `AuthExceptionHandler` conservé sans modification (pas de régression)
- [ ] Configuration SpringDoc/OpenAPI dans `application.yml` (info API, contact, version)
- [ ] Swagger UI accessible publiquement sans auth (ou avec auth selon config)
- [ ] Tests unitaires `GlobalExceptionHandlerTest` (cas : ResponseStatusException, validation, auth, generic)
- [ ] Build backend sans erreur, tous les tests passent

## 3. Contexte analysé

- `AuthExceptionHandler` existe déjà (`@RestControllerAdvice`) — utilise `ProblemDetail` RFC 7807
- Aucun `trace_id` n'existe dans les réponses
- `springdoc-openapi-starter-webmvc-ui` v2.8.5 est dans `pom.xml` mais non configuré
- Aucune config SpringDoc dans `application.yml`
- Les controllers n'ont pas d'annotations OpenAPI (sauf `FhirController`)

## 4. Hypothèses

- Le format `ProblemDetail` RFC 7807 de `AuthExceptionHandler` est conservé pour ne pas casser les clients existants
- Le `GlobalExceptionHandler` ajoute un **nouveau** format CDC `{error: {code, message, trace_id}}` en complément
- La config SpringDoc est minimale (info, version, contact) pour le pilote

## 5. Risques et impacts

| Risque | Impact | Mitigation |
|---|---|---|
| Régression réponses d'erreur existantes | Moyen | Conserver `AuthExceptionHandler`, ajouter `GlobalExceptionHandler` pour les cas non couverts |
| Conflit entre deux `@RestControllerAdvice` | Moyen | `GlobalExceptionHandler` avec `order` supérieur, `AuthExceptionHandler` reste primaire pour auth |

## 6. Fichiers à créer/modifier

- **Nouveau** : `backend/src/main/java/com/joprelys/backend/common/api/GlobalExceptionHandler.java`
- **Nouveau** : `backend/src/main/java/com/joprelys/backend/common/api/ApiErrorResponse.java`
- **Nouveau** : `backend/src/main/java/com/joprelys/backend/common/api/TraceIdFilter.java`
- **Nouveau** : `backend/src/test/java/com/joprelys/backend/common/api/GlobalExceptionHandlerTest.java`
- **Modifier** : `backend/src/main/resources/application.yml` — ajouter config springdoc
- **Modifier** : `backend/src/main/java/com/joprelys/backend/auth/config/SecurityConfig.java` — autoriser `/swagger-ui/**` et `/v3/api-docs/**` si nécessaire

## 7. Statut

Statut : DONE

## 9. Implémentation réalisée

### Sous-tâche 1 — SpringDoc/OpenAPI
- Configuration `springdoc` dans `application.yml` (titre, version, contact, Swagger UI `/swagger-ui`)
- Déjà présent dans `SecurityConfig` : `/swagger-ui/**` et `/v3/api-docs/**` autorisés publiquement

### Sous-tâche 2 — Erreurs normalisées + trace_id
- `TraceIdFilter` — génère `X-Trace-Id` par requête, propagé dans MDC et headers réponse
- `ApiErrorResponse` — DTO format CDC `{ error: { code, message, trace_id } }`
- `GlobalExceptionHandler` — `@RestControllerAdvice` gérant 5 types d'exceptions avec `trace_id`
- `AuthExceptionHandler` conservé sans modification (pas de régression)
- Tests : `GlobalExceptionHandlerTest` (7 cas)

### Sous-tâche 3 — Rate limiting
- `RateLimitingFilter` — bucket token simplifié par IP / utilisateur
- `RateLimitingProperties` — `@ConfigurationProperties` (`enabled`, `max-requests-per-window`, `window-seconds`)
- Désactivé en profil `test` via `application-test.yml`
- Tests : `RateLimitingFilterTest` (5 cas)

### Sous-tâche 4 — Webhooks
- `WebhookEntity` — entité JPA avec validation HTTPS, suppression logique
- `WebhookRepository` — requêtes par org+status et par status
- `WebhookService` — CRUD + déclenchement simulé (log mock)
- `WebhookController` — endpoints REST (`/api/webhooks`), rôles ADMIN_CLINIQUE/ADMIN_JOPRELYS
- Migration Flyway `V27__webhooks_schema.sql`
- Tests : `WebhookServiceTest` (9 cas)

### Sous-tâche 5 — Annotations OpenAPI
- `@Tag`, `@Operation`, `@ApiResponse`, `@Parameter` sur 6 controllers principaux :
  `PatientController`, `VisitController`, `ConsultationController`, `PrescriptionController`, `LabOrderController`, `DocumentController`

## 10. Tests et vérifications

- `./mvnw test` → BUILD SUCCESS — 226 tests, 0 échec, 0 erreur

## 11. Reste à faire

- Aucun (Module 16 API complet pour le pilote).

## 8. Impact version / SemVer

| Champ | Valeur |
|---|---|
| Type de bump | MINOR |
| Breaking change | Non (format ProblemDetail conservé) |
| Changement API | Oui (nouveau format d'erreur, Swagger UI) |
