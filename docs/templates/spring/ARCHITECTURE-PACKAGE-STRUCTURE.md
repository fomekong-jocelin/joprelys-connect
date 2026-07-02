# Spring Boot Package Structure — Maven / SOLID / Backend maître

Structure recommandée par feature :

```text
src/main/java/<base>/<feature>/
  api/
    <Feature>Controller.java
    dto/
      Request/Response DTOs
  application/
    <Action>UseCase.java
    impl/
      Default<Action>UseCase.java
  domain/
    Entity / ValueObject / Policy / Ports
  infrastructure/
    persistence/
    external/
    messaging/
```

Règles :

- Maven obligatoire.
- `application.yml` obligatoire.
- Controller = HTTP + validation + délégation.
- Service/use case = orchestration + transaction.
- Domain = règles métier.
- Infrastructure = détails techniques.
- Pas d’entités JPA dans les réponses API.
- Pas de logique métier côté front comme source de vérité.
