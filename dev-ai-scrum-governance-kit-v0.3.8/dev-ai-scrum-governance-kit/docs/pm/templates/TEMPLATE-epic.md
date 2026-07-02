# EPIC-<id> — <Titre>

## 1. Objectif métier

Décrire la valeur métier attendue.

## 2. Périmètre

### Inclus

- 

### Exclus

- 

## 3. Utilisateurs concernés

- 

## 4. User stories

| ID | Titre | Priorité | SP | Statut | Sprint cible |
|---|---|---|---:|---|---|
| STORY- | | | | | |

## 5. Dépendances

| Dépendance | Type | Impact |
|---|---|---|
| | | |

## 6. Risques

| Risque | Impact | Mitigation |
|---|---|---|
| | | |

## 7. Définition de succès

- [ ] Succès 1
- [ ] Succès 2

## 8. Estimation globale

| Élément | Valeur |
|---|---|
| Total story points | |
| Effort senior | |
| Effort intermédiaire | |
| Effort junior | |
| Nombre de sprints estimé | |


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
- Frontend Angular : Tailwind CSS v4 uniquement ; Angular Material interdit sauf ADR validée.
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

## Design System / UI

À remplir si la tâche touche Angular, Flutter ou une interface utilisateur :

- [ ] `DESIGN.md` lu ou créé
- [ ] Tokens couleurs/typo/spacing/radius impactés
- [ ] Composants réutilisables identifiés
- [ ] Tailwind CSS v4 vérifié côté Angular
- [ ] Tailwind v3 / Angular Material absents sauf ADR
- [ ] Mapping Flutter ThemeData/tokens prévu si mobile
- [ ] Light/dark vérifiés
- [ ] i18n FR/EN prévue
- [ ] Contraste/focus/accessibilité vérifiés


## Architecture / SOLID

- [ ] Backend maître de la vérité métier identifié.
- [ ] Front/mobile limité à l’affichage, l’expérience utilisateur et l’état de présentation.
- [ ] Controllers sans logique métier.
- [ ] Services/use cases, interfaces et implémentations prévus si nécessaire.
- [ ] Généricité, héritage et abstractions justifiés si utilisés.
- [ ] Aucune classe/composant/widget ne doit dépasser 500 lignes.
