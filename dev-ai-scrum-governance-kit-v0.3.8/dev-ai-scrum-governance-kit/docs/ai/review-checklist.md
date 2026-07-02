# Production-Ready Review Checklist

> Checklist obligatoire pour chaque Pull Request, Merge Request ou intervention IA.
> Tout élément doit être marqué ✅, ❌ ou ➖ avec justification.
> Cette checklist s’applique à Spring Boot, Angular, Flutter, CI/CD, sécurité, tests, observabilité et documentation.

---

> **Use this checklist for every Pull Request / Merge Request.**
> Every item must be verified. Mark ✅ (pass), ❌ (fail — must fix), or ➖ (N/A with justification).

---

## 1. Architecture & Design


### 1.0 SOLID strict et backend maître

- [ ] `docs/standards/ARCHITECTURE-SOLID-RESPONSIBILITY-STANDARDS.md` a été appliqué.
- [ ] SOLID est respecté : SRP, OCP, LSP, ISP, DIP.
- [ ] Le backend porte la vérité métier : validation, décisions, autorisations, calculs, persistance.
- [ ] Le front Angular/Flutter ne contient aucune logique métier critique non validée côté backend.
- [ ] Les controllers Spring Boot ne contiennent aucune logique métier.
- [ ] Les controllers ne dépendent pas directement de repositories, SQL, clients HTTP externes ou composants infrastructure.
- [ ] Les controllers délèguent à des services/use cases avec contrats clairs.
- [ ] Les services/use cases portent l’orchestration et les transactions.
- [ ] Le domain porte les invariants, policies et règles métier, sans dépendance framework.
- [ ] Les interfaces sont petites et orientées usage ; aucune interface fourre-tout.
- [ ] La généricité est utile, typée et ne cache pas le vocabulaire métier.
- [ ] L’héritage ou les classes abstraites sont justifiés ; pas d’héritage profond ni d’abstraction spéculative.
- [ ] Les composants Angular/Flutter sont orientés présentation et découpés en composants/widgets réutilisables.
- [ ] Toute classe/composant/widget > 300 lignes est signalé pour extraction.
- [ ] Toute classe/composant/widget > 500 lignes est rejeté sauf ADR temporaire.

### 1.1 Separation of Responsibilities

- [ ] Each class/component has a **single responsibility**. No god classes.
- [ ] **No class exceeds 500 lines.** Classes approaching 300 lines should be flagged for extraction.
- [ ] **No method exceeds 40 lines.** Extract helper methods or delegate to collaborators.
- [ ] Controller/Component layer contains **zero business logic** — only input validation, delegation, and response mapping.
- [ ] Domain/business layer has **zero framework imports** (no Spring, Angular, Flutter dependencies).
- [ ] Infrastructure layer implements domain interfaces — never the reverse.
- [ ] New abstractions are justified. No premature generalization or speculative architecture.

### 1.2 Layer Boundaries

**Backend (Spring Boot 4.0.x):**
- [ ] **Build tool standard: Maven only.** Project uses `pom.xml` and `mvnw`; no new `build.gradle`, `settings.gradle` or `./gradlew` is introduced without an accepted ADR.
- [ ] **Spring configuration standard: YAML only.** Project uses `src/main/resources/application.yml` and profile files `application-<profile>.yml`; no new `application.properties` is introduced without an accepted ADR and migration plan.
- [ ] Strict `Controller → Service → Domain ← Infrastructure` dependency flow.
- [ ] JPA entities are never exposed in API responses. DTOs (`record` types) are used.
- [ ] `@Transactional` is on service methods only (not controllers, not repositories).
- [ ] Domain events are used for cross-aggregate communication (not direct service calls).

**Frontend (Angular 22):**
- [ ] **Tailwind CSS v4 is the mandatory Angular UI standard.** New Angular UI is implemented with Tailwind v4 CSS-first, `@theme`, shared components and the internal design system.
- [ ] **Angular Material is forbidden by default.** No `@angular/material`, `Mat*` modules, `mat-*` components or Material theme is introduced without an accepted ADR.
- [ ] **Angular dev proxy is mandatory.** A versioned `proxy.conf.json` exists and is referenced in `angular.json` through `proxyConfig`.
- [ ] **No hardcoded backend URL in Angular services.** API calls use relative paths (`/api`, `/auth`, etc.) routed by the proxy in development and by the deployment gateway/reverse proxy in production.
- [ ] Smart (container) components inject services; dumb (presentational) components use `@Input` / `@Output` only.
- [ ] Services encapsulate all API calls and business logic — components do not call `HttpClient` directly.
- [ ] State is managed via `signal()`, `computed()`, `effect()` — not manual `ChangeDetectorRef`.
- [ ] Shared state lives in services or stores, not in component hierarchies via `@Input` drilling.

**Mobile (Flutter 3.44):**
- [ ] Feature-first folder structure: `domain/`, `data/`, `presentation/` per feature.
- [ ] Use cases are the only entry point from presentation to domain.
- [ ] Repository interfaces are in domain; implementations are in data.
- [ ] Widgets that exceed 200 lines are split into sub-widget classes (not builder methods).

### 1.3 Theme, i18n & Branding

**Angular & Flutter:**
- [ ] Theme management is centralized in a service/controller/design system.
- [ ] `DESIGN.md` exists or is updated for significant UI changes.
- [ ] No Tailwind v3 syntax (`@tailwind base/components/utilities`) or v3 token source (`tailwind.config.js`) is introduced without ADR.
- [ ] Both `light` and `dark` themes are implemented or preserved.
- [ ] UI is decomposed into reusable components/widgets; no duplicated UI blocks.
- [ ] Internationalization is implemented with at least French (`fr`) and English (`en`).
- [ ] No user-visible text is hardcoded in components/templates/widgets.
- [ ] App name, logo, slogan, publisher info, public links and display settings are centralized in app configuration.
- [ ] Loading, empty and error states use reusable UI components.
- [ ] Accessibility basics are checked for both themes: contrast, focus, labels.

---

## 2. API Contract & Data Flow

### 2.1 Request/Response Design

- [ ] All DTOs are immutable (`record` in Java, `readonly` in TypeScript, `@freezed` in Dart).
- [ ] API follows consistent naming conventions (RESTful nouns, consistent casing).
- [ ] Pagination is implemented for all list endpoints (cursor-based preferred, offset acceptable).
- [ ] API versioning strategy is applied (`spring.mvc.apiversion.*` for Spring Boot 4).
- [ ] Error responses follow RFC 9457 Problem Detail format (backend) or structured error DTOs.

### 2.2 Input Validation

- [ ] **All inputs validated at boundary** — controllers (`@Valid`), components (form validators), widgets (text input formatters).
- [ ] Custom validation logic is extracted to dedicated `*Validator` classes.
- [ ] Validation error messages are user-friendly and localized.
- [ ] File uploads: validate type, size, and content (not just extension).
- [ ] No raw user input is passed to SQL, OS commands, file paths, or template engines.

### 2.3 HTTP Contracts

- [ ] Correct HTTP status codes: `200` (OK), `201` (Created), `204` (No Content), `400` (Bad Request), `401` (Unauthorized), `403` (Forbidden), `404` (Not Found), `409` (Conflict), `422` (Unprocessable), `429` (Rate Limited), `500` (Server Error).
- [ ] `Location` header set for `201 Created` responses.
- [ ] `ETag` / `If-None-Match` used for cacheable resources.
- [ ] No breaking changes to existing API contracts without version bump.

---

## 3. Persistence & Data

### 3.1 Database

- [ ] All queries use parameterized statements. No string concatenation for SQL.
- [ ] N+1 query problem is addressed: use `JOIN FETCH`, `@EntityGraph`, or batch loading.
- [ ] Indexes exist for all foreign keys and commonly filtered/sorted columns.
- [ ] Pagination is pushed to the database (`LIMIT`/`OFFSET` or keyset pagination).
- [ ] Connection pool is configured and monitored (`HikariCP` settings explicit in config).
- [ ] Read-only queries use `@Transactional(readOnly = true)` for performance.

### 3.2 Migrations

- [ ] Database migrations are version-controlled (Flyway / Liquibase).
- [ ] Migrations are forward-only and backward-compatible (add before remove, two-phase migration).
- [ ] Destructive changes (column drops, type changes) have a migration plan with rollback steps.
- [ ] Migration scripts are tested in CI against a real database (Testcontainers).

### 3.3 Caching

- [ ] Cache keys are deterministic and include version/tenant where applicable.
- [ ] Cache invalidation strategy is documented and tested.
- [ ] Cache TTL is configured — no indefinite caching without explicit justification.
- [ ] Sensitive data is never cached in client-side or shared caches.

---

## 4. Security

### 4.1 Authentication & Authorization

- [ ] AuthN/AuthZ is enforced on **every** endpoint (deny by default).
- [ ] Method-level security (`@PreAuthorize`, route guards, middleware) for fine-grained access.
- [ ] Principle of least privilege: users/services get minimum required permissions.
- [ ] JWT tokens validate: issuer, audience, expiration, signature algorithm, and not-before claim.
- [ ] Refresh tokens use rotation — old tokens are invalidated on use.
- [ ] Session timeout is configured and enforced.
- [ ] Admin/sensitive endpoints require step-up authentication or MFA.
- [ ] Account lockout after configurable failed attempts (default: 5 in 15 minutes).
- [ ] Rate limiting on authentication and password reset endpoints.

### 4.2 Data Protection

- [ ] All communication over TLS 1.3. No fallback to older TLS versions.
- [ ] Sensitive data encrypted at rest (AES-256-GCM or equivalent).
- [ ] Passwords hashed with Argon2id or bcrypt (cost ≥ 12). Never MD5/SHA for passwords.
- [ ] PII is masked in logs and error responses.
- [ ] GDPR/CCPA: data retention policies implemented, right-to-erasure supported.
- [ ] File uploads scanned for malware before processing.

### 4.3 Secrets

- [ ] **Zero secrets in source code**, config files, or environment variable defaults.
- [ ] Secrets loaded from vault (HashiCorp Vault, AWS SM, GCP SM) or CI/CD secret injection.
- [ ] `.env` files are in `.gitignore`. `.env.example` exists with placeholder values.
- [ ] API keys and tokens have expiration dates and scoped permissions.
- [ ] No secrets in log output (even at `DEBUG` level).
- [ ] Secret rotation process is documented and tested.

### 4.4 Headers & Browser Security (Web)

- [ ] `Content-Security-Policy` (CSP) configured — no `unsafe-inline` or `unsafe-eval` without justification.
- [ ] `Strict-Transport-Security` (HSTS) enabled with `max-age ≥ 31536000`.
- [ ] `X-Content-Type-Options: nosniff` set.
- [ ] `X-Frame-Options: DENY` or `SAMEORIGIN` set.
- [ ] `Referrer-Policy: strict-origin-when-cross-origin` or stricter.
- [ ] CORS configured with explicit allowed origins — never `*` in production.

### 4.5 Mobile Security (Flutter)

- [ ] Release builds are obfuscated (`--obfuscate --split-debug-info`).
- [ ] Certificate pinning enabled for API connections.
- [ ] Sensitive data stored in `flutter_secure_storage`, never `SharedPreferences`.
- [ ] No logging of sensitive data in release builds.
- [ ] ProGuard/R8 enabled for Android release builds.
- [ ] Root/jailbreak detection for high-security apps.

---

## 5. Testing

### 5.1 Coverage & Quality

- [ ] **Domain/business logic: ≥80% line coverage** (target 95%).
- [ ] **Security paths (AuthN/AuthZ): 100% coverage** — both allowed and denied.
- [ ] **Controllers/components: ≥70% coverage** (target 85%).
- [ ] No test without at least one meaningful assertion.
- [ ] Tests follow AAA pattern (Arrange → Act → Assert).
- [ ] Test names are descriptive: `should_<expected>_when_<condition>`.
- [ ] No `@Disabled` / `skip` without a linked tracking issue.

### 5.2 Unit Tests

- [ ] Pure business logic tested with zero I/O dependencies (mocked).
- [ ] Edge cases covered: null/empty, boundary values, invalid formats, concurrent access.
- [ ] Error paths tested: exception types, error messages, fallback behavior.
- [ ] Unit test suite runs in < 30 seconds.

### 5.3 Integration Tests

- [ ] API endpoints tested with `@WebMvcTest` or equivalent HTTP test framework.
- [ ] Database interactions tested with Testcontainers (real DB, not H2 in production with PostgreSQL).
- [ ] External service calls tested with WireMock or equivalent mock server.
- [ ] Message broker interactions (Kafka, RabbitMQ) tested with Testcontainers.
- [ ] Integration test suite runs in < 10 minutes.

### 5.4 Frontend & Mobile Tests

- [ ] Every component/widget has unit tests for inputs, outputs, and user interactions.
- [ ] Route guards and interceptors tested for both allowed and denied scenarios.
- [ ] Golden image tests for design-system components (Flutter).
- [ ] Signal-based components tested using Angular 22 signal test helpers.
- [ ] Riverpod providers tested with `ProviderContainer` and mocked dependencies.

### 5.5 E2E / Acceptance Tests

- [ ] Critical user journeys covered (login, core workflow, checkout, data sync).
- [ ] E2E tests run against staging environment in CI pipeline.
- [ ] Visual regression tests for UI-critical flows.
- [ ] Performance benchmarks for critical paths with defined thresholds.

---

## 6. Observability

### 6.1 Logging

- [ ] Structured JSON logs in production. Key fields: `timestamp`, `level`, `correlationId`, `service`, `message`.
- [ ] Correlation ID propagated across service boundaries (HTTP headers, message metadata).
- [ ] Log levels are appropriate: `ERROR` (action required), `WARN` (degraded), `INFO` (business events), `DEBUG` (disabled in prod).
- [ ] **No PII, secrets, tokens, or full stack traces in log output.**
- [ ] Log rotation and retention configured (30 days minimum for audit, 7 days for debug).

### 6.2 Metrics

- [ ] Request latency (p50, p95, p99) tracked for all API endpoints.
- [ ] Error rate tracked and alertable.
- [ ] Throughput (requests/sec) tracked.
- [ ] Resource utilization: DB connection pool, thread pool, memory, CPU.
- [ ] Custom business metrics: domain-specific KPIs (orders/sec, conversion rate, etc.).

### 6.3 Tracing

- [ ] Distributed tracing enabled (OpenTelemetry / Micrometer / `spring-boot-starter-opentelemetry`).
- [ ] Trace context propagated through HTTP, messaging, and async boundaries.
- [ ] Critical business operations have custom spans with relevant attributes.
- [ ] Slow queries (> 200ms) are flagged in traces.

### 6.4 Health Checks & Alerts

- [ ] Health endpoints: `/health/liveness` (app is running) and `/health/readiness` (app can serve traffic).
- [ ] Dependency health: database, cache, external services included in readiness check.
- [ ] Alerting rules defined for: error rate > 1%, p99 > 2s, disk > 85%, cert expiry < 30d.
- [ ] Every alert has a linked runbook with triage steps.

---

## 7. Performance

- [ ] Database queries are profiled — no unindexed full table scans on large tables.
- [ ] Lazy loading used for below-fold UI content and non-critical data.
- [ ] Images are optimized (WebP, responsive sizing, lazy loading).
- [ ] API responses are compressed (gzip/brotli).
- [ ] Connection pooling configured for database and HTTP clients.
- [ ] Virtual threads (`spring.threads.virtual.enabled=true`) used for I/O-bound Spring Boot workloads.
- [ ] Bundle size monitored (Angular). Tree-shaking and code splitting verified.
- [ ] Flutter widget rebuilds minimized — `const` constructors, `RepaintBoundary` where applicable.
- [ ] No blocking operations on UI thread (Flutter) or main zone (Angular).

---

## 8. Documentation & Maintainability

- [ ] All public APIs have doc comments explaining **what** and **why**.
- [ ] API documentation auto-generated (OpenAPI/Swagger for REST, compodoc for Angular).
- [ ] README updated with setup instructions, architecture overview, and contribution guide.
- [ ] Architecture Decision Records (ADRs) created for significant design choices.
- [ ] Deprecated code is annotated with `@Deprecated` and migration path, with removal deadline.
- [ ] Changelog updated for user-facing changes.
- [ ] Code comments explain **why** (rationale), not **what** (obvious from code).

---

## 9. CI/CD & Deployment

- [ ] All pipeline stages pass: lint → build → unit test → integration test → security scan → coverage gate.
- [ ] Docker images use multi-stage builds with non-root user.
- [ ] Container images scanned for vulnerabilities (Trivy, Snyk).
- [ ] Environment-specific config is externalized (not baked into artifacts).
- [ ] Database migrations run automatically before application startup (with rollback plan).
- [ ] Blue/green or canary deployment strategy configured.
- [ ] Rollback procedure tested and documented.
- [ ] Feature flags used for progressive rollouts of risky changes.
- [ ] Semantic versioning applied. Git tags created for every release.

---

## 10. Compliance & Governance

- [ ] SBOM (Software Bill of Materials) generated and stored with release artifacts.
- [ ] License compliance checked — no GPL-incompatible libraries in proprietary code.
- [ ] Data residency requirements met (if applicable).
- [ ] Accessibility (a11y) standards met: WCAG 2.2 AA for web, platform guidelines for mobile.
- [ ] Privacy by design: data minimization, purpose limitation, consent management.

---

## Quick Reference: Rejection Criteria

> **Immediately reject** a PR/MR if any of the following are true:

| # | Violation |
|---|---|
| 1 | Any class/component/widget exceeds **500 lines**. |
| 2 | Business logic exists in controller, entity, config, or UI layer. |
| 3 | Secrets (API keys, passwords, tokens) found in source code or logs. |
| 4 | SQL injection vector: string concatenation in queries. |
| 5 | Security endpoint (AuthN/AuthZ) lacks test coverage. |
| 6 | `@Disabled` / `skip` test without linked tracking issue. |
| 7 | No input validation on user-facing endpoint. |
| 8 | PII or sensitive data logged without masking. |
| 9 | Breaking API change without version bump. |
| 10 | Test coverage drops below defined thresholds. |
| 11 | Gradle introduced in a Spring Boot backend without accepted ADR. |
| 12 | Angular Material introduced in an Angular frontend without accepted ADR. |
| 12b | Tailwind CSS v3, v3 syntax or `tailwind.config.js` token source introduced without accepted ADR. |
| 12c | Significant UI created or changed without reading/updating `DESIGN.md`. |
| 13 | New `application.properties` introduced in a Spring Boot backend without accepted ADR. |
| 14 | Angular project has no `proxy.conf.json` or no `proxyConfig` in `angular.json`. |
| 15 | Angular service hardcodes a backend host/port instead of using relative API paths. |
| 13 | New Angular/Flutter UI without centralized theme integration. |
| 14 | Missing `light` / `dark` theme support for user-facing UI. |
| 15 | User-visible strings hardcoded instead of i18n `fr` / `en`. |
| 16 | App name, logo or branding hardcoded in screens instead of centralized config. |
| 17 | Duplicated UI block where a reusable component/widget is required. |


---

## 11. Documentation First

- [ ] La documentation fonctionnelle de la feature existe avant ou avec le développement.
- [ ] La documentation technique de la feature existe avant ou avec le développement.
- [ ] Les critères d’acceptation sont documentés et alignés avec les tests.
- [ ] Les contrats API sont documentés si endpoint, DTO ou erreur API modifiés.
- [ ] Le modèle de données et les migrations sont documentés si DB impactée.
- [ ] Les impacts configuration sont documentés (`application.yml`, proxy Angular, app config, Flutter config).
- [ ] Les impacts UI/i18n/thème/branding sont documentés si Angular ou Flutter impactés.
- [ ] Le changelog et la décision SemVer sont à jour si changement livrable.

### Critères de rejet immédiat — Documentation

| # | Violation |
|---|---|
| D1 | Code livré sans documentation fonctionnelle minimale. |
| D2 | Changement technique livré sans documentation technique minimale. |
| D3 | API modifiée sans contrat ou documentation d’erreur. |
| D4 | Migration DB non documentée. |
| D5 | Configuration ajoutée sans explication. |
| D6 | UI/mobile livré sans textes FR/EN ou sans impact thème/branding documenté. |

## Vérification UI — Arrondis et ombres

- [ ] `docs/standards/UI-RADIUS-AND-SHADOW-STANDARDS.md` lu si UI Angular/Flutter.
- [ ] Les cards, formulaires, inputs et boutons utilisent des coins carrés ou légèrement arrondis.
- [ ] Aucun usage injustifié de `rounded-full`, `rounded-2xl`, `rounded-3xl` ou équivalent.
- [ ] Les rayons sont centralisés dans les tokens design/Tailwind v4/Flutter theme.
- [ ] Les ombres sont sobres et cohérentes light/dark.
- [ ] Toute exception est documentée dans `DESIGN.md` ou ADR.

| 11 | Logique métier critique uniquement côté Angular/Flutter. |
| 12 | Controller Spring Boot qui appelle directement repository, SQL ou client externe. |
| 13 | Abstraction, héritage ou service générique introduit sans besoin réel. |
| 14 | Interface fourre-tout ou service monolithique. |


## Règle obligatoire — `.gitignore`

Chaque projet doit avoir un `.gitignore` à la racine, adapté à sa stack réelle.

L’agent doit vérifier ce fichier avant toute livraison et appliquer :

```text
docs/standards/GITIGNORE-STANDARDS.md
```

Règles minimales :

- ne jamais versionner les secrets ;
- ignorer `target/`, `node_modules/`, `dist/`, `build/`, `.dart_tool/`, caches et logs ;
- conserver les fichiers de gouvernance IA ;
- conserver `proxy.conf.json` côté Angular sauf variante locale ;
- adapter le `.gitignore` au projet réel au lieu de copier aveuglément un template.
