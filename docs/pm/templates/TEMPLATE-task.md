# TASK-<id> — <Titre>

## 1. Objectif

Décrire l'objectif précis de cette tâche.

## 2. Stack concernée

- [ ] Spring Boot
- [ ] Angular
- [ ] Flutter
- [ ] Base de données
- [ ] CI/CD
- [ ] Documentation

## 3. Critères d'acceptation

- [ ] Critère 1
- [ ] Critère 2

## 4. Estimation

| Champ | Valeur |
|---|---|
| Story points | |
| Profil recommandé | |
| Effort senior | |
| Effort intermédiaire | |
| Effort junior | |
| Assigné | |
| Reviewer | |
| Sprint | |

## 5. Checklist technique

- [ ] Existant analysé
- [ ] Impacts identifiés
- [ ] Tests prévus
- [ ] Sécurité vérifiée
- [ ] Documentation prévue

## 6. Suivi

| Date | Temps passé | Avancement | Reste à faire | Blocage | Commentaire |
|---|---:|---:|---:|---|---|
| | | | | | |

## 7. Tests

```bash
# commandes
```

## 8. Statut

BACKLOG / READY / TODO / IN_PROGRESS / BLOCKED / REVIEW / QA / DONE / CANCELLED


## Impact version / SemVer

| Champ | Valeur |
|---|---|
| Impact version | Aucun / PATCH / MINOR / MAJOR |
| Justification | |
| Breaking change | Oui / Non |
| Release cible | |


## Contraintes techniques standard

- Backend Spring Boot : Maven uniquement (`pom.xml`, `mvnw`).
- Backend Spring Boot : configuration YAML obligatoire (`application.yml`, `application-<profile>.yml`), pas de `application.properties`.
- Frontend Angular : Tailwind CSS uniquement ; Angular Material interdit sauf ADR validée.
- Frontend Angular : `proxy.conf.json` obligatoire, référencé dans `angular.json`; API appelée via chemins relatifs.

## Impact UI / thème / i18n / configuration

| Point | Valeur |
|---|---|
| Impact Angular | Oui / Non |
| Impact Flutter | Oui / Non |
| Thème centralisé impacté | Oui / Non |
| Dark/light à vérifier | Oui / Non |
| Traductions FR/EN nécessaires | Oui / Non |
| Composants/widgets réutilisables à créer | Oui / Non |
| Configuration app/branding impactée | Oui / Non |
| Logo / nom app / slogan impactés | Oui / Non |


## Documentation First

- [ ] Documentation fonctionnelle initiale créée / mise à jour : `docs/features/<feature-id>/FUNCTIONAL-SPEC.md`
- [ ] Documentation technique initiale créée / mise à jour : `docs/features/<feature-id>/TECHNICAL-DESIGN.md`
- [ ] `API-CONTRACT.md` créé / mis à jour si API impactée
- [ ] `DATA-MODEL.md` créé / mis à jour si base de données impactée
- [ ] `TEST-PLAN.md` créé / mis à jour selon les tests attendus
- [ ] `USER-GUIDE.md` créé / mis à jour si impact utilisateur final
- [ ] Documentation relue en review
- [ ] Documentation à jour avant passage à DONE
