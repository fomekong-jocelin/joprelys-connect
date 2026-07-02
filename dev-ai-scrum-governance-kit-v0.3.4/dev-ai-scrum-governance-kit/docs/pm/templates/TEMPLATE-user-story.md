# STORY-<id> — <Titre>

## 1. User story

En tant que `<profil utilisateur>`, je veux `<capacité>`, afin de `<valeur métier>`.

## 2. Critères d'acceptation

- [ ] Critère 1
- [ ] Critère 2
- [ ] Critère 3

## 3. Périmètre

### Inclus

- 

### Exclus

- 

## 4. Tâches

| ID | Titre | Stack | Profil recommandé | SP | Estimation | Statut |
|---|---|---|---|---:|---:|---|
| TASK- | | | | | | |

## 5. Estimation

| Champ | Valeur |
|---|---|
| Story points | |
| Complexité | XS / S / M / L / XL |
| Profil recommandé | |
| Effort senior | |
| Effort intermédiaire | |
| Effort junior | |
| Risque | Faible / Moyen / Fort |

## 6. Definition of Ready

- [ ] Critères d'acceptation clairs
- [ ] Dépendances connues
- [ ] Données de test disponibles
- [ ] Profil recommandé identifié
- [ ] Estimation faite
- [ ] Reviewer identifié

## 7. Definition of Done

- [ ] Code terminé
- [ ] Tests OK
- [ ] Review OK
- [ ] QA OK
- [ ] Documentation mise à jour si nécessaire
- [ ] Changelog mis à jour si nécessaire


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
