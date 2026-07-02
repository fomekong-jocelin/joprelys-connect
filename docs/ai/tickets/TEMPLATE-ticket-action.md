# TICKET-<id> — <Titre>

> Fichier obligatoire pour chaque ticket ou intervention IA.

## 1. Objectif

Décrire le besoin métier ou technique.

## 2. Critères d'acceptation

- [ ] Critère 1
- [ ] Critère 2
- [ ] Critère 3

## 3. Pilotage projet

| Champ | Valeur |
|---|---|
| Epic parent | |
| User story parent | |
| Sprint cible | |
| Priorité business | P0 / P1 / P2 / P3 |
| Complexité | XS / S / M / L / XL |
| Story points | 1 / 2 / 3 / 5 / 8 / 13 |
| Profil recommandé | Junior / Intermédiaire / Senior / Tech Lead |
| Effort estimé senior | |
| Effort estimé intermédiaire | |
| Effort estimé junior | |
| Responsable | |
| Reviewer obligatoire | |
| Risque fonctionnel | Faible / Moyen / Fort |
| Risque technique | Faible / Moyen / Fort |
| Dépendances | |
| Bloquants connus | |

## 4. Contexte analysé

- [ ] `AGENTS.md` lu
- [ ] `SKILL.md` lu
- [ ] `PROJECT-MANAGER-SKILL.md` lu si nécessaire
- [ ] `README-IA.md` lu
- [ ] `WORKFLOW-IA.md` lu
- [ ] `PROJECT-TRACKING.md` lu
- [ ] `CHANGELOG.md` lu
- [ ] `review-checklist.md` lu
- [ ] Code existant analysé
- [ ] Tests existants analysés
- [ ] Contrats API analysés
- [ ] Impacts sécurité analysés
- [ ] Impacts données analysés
- [ ] Impacts Angular analysés si applicable
- [ ] Impacts Flutter analysés si applicable
- [ ] Impacts backend analysés si applicable
- [ ] Backend Maven uniquement vérifié si applicable
- [ ] Backend `application.yml` / profils YAML vérifiés ; aucun nouveau `application.properties`
- [ ] Frontend Tailwind CSS vérifié si applicable
- [ ] Absence Angular Material vérifiée si applicable
- [ ] Angular `proxy.conf.json` présent et référencé dans `angular.json` si applicable
- [ ] Aucun appel API Angular avec URL backend hardcodée
- [ ] Capacité sprint analysée si applicable

## 5. Hypothèses

- Hypothèse 1
- Hypothèse 2

## 6. Risques et impacts

| Risque | Impact | Mitigation |
|---|---|---|
|  |  |  |

## 7. Action plan

- [ ] Action 1
- [ ] Action 2
- [ ] Action 3
- [ ] Ajouter ou modifier les tests
- [ ] Exécuter les vérifications
- [ ] Mettre à jour la documentation
- [ ] Mettre à jour `CHANGELOG.md`
- [ ] Mettre à jour `PROJECT-TRACKING.md`
- [ ] Mettre à jour les documents PM si impact planning

## 8. Implémentation réalisée

- [ ] Élément réalisé 1
- [ ] Élément réalisé 2
- [ ] Élément réalisé 3

## 9. Suivi d'exécution

| Date | Développeur | Temps passé | Avancement | Reste à faire | Blocage | Commentaire |
|---|---|---:|---:|---:|---|---|
| YYYY-MM-DD | | | | | | |

## 10. Tests et vérifications

### Commandes exécutées ou à exécuter

```bash
# Backend
./mvnw test
./mvnw clean verify

# Angular
npm run lint
npm run test
npm run build

# Flutter
flutter analyze
flutter test
```

### Résultats

- [ ] Tests unitaires OK
- [ ] Tests intégration OK
- [ ] Tests UI/widget OK
- [ ] Tests sécurité OK
- [ ] Build OK
- [ ] Analyse statique OK
- [ ] Non exécuté avec justification

## 11. Documentation

- [ ] README mis à jour si nécessaire
- [ ] API docs mises à jour si nécessaire
- [ ] ADR créé si décision structurante
- [ ] Changelog mis à jour
- [ ] Suivi projet mis à jour
- [ ] Suivi sprint/capacité mis à jour si nécessaire

## 12. Reste à faire

- [ ] Élément restant 1
- [ ] Élément restant 2

## 13. Statut final

Statut : BACKLOG / READY / TODO / IN_PROGRESS / BLOCKED / REVIEW / QA / DONE / CANCELLED

## 14. Notes finales

Résumer ici ce que la prochaine IA, le prochain développeur ou le chef de projet doit absolument savoir.


## 13. Impact version / SemVer

| Champ | Valeur |
|---|---|
| Changement livrable | Oui / Non |
| Type de bump | Aucun / PATCH / MINOR / MAJOR |
| Justification | |
| Breaking change | Oui / Non |
| Migration DB | Oui / Non |
| Changement API | Oui / Non |
| Impact Angular | Oui / Non |
| Impact Flutter | Oui / Non |
| Changelog requis | Oui / Non |
| Release note requise | Oui / Non |



## 4.1 Impact thème / i18n / branding

- [ ] Impact Angular UI analysé
- [ ] Impact Flutter UI analysé
- [ ] Thème centralisé vérifié
- [ ] Thèmes light/dark vérifiés
- [ ] Textes `fr` / `en` prévus
- [ ] Composants/widgets réutilisables prévus
- [ ] Configuration app/branding vérifiée : nom, logo, slogan, éditeur, liens, paramètres publics
- [ ] Aucun texte ou branding hardcodé prévu



## Documentation First

- [ ] Documentation fonctionnelle initiale créée / mise à jour : `docs/features/<feature-id>/FUNCTIONAL-SPEC.md`
- [ ] Documentation technique initiale créée / mise à jour : `docs/features/<feature-id>/TECHNICAL-DESIGN.md`
- [ ] `API-CONTRACT.md` créé / mis à jour si API impactée
- [ ] `DATA-MODEL.md` créé / mis à jour si base de données impactée
- [ ] `TEST-PLAN.md` créé / mis à jour selon les tests attendus
- [ ] `USER-GUIDE.md` créé / mis à jour si impact utilisateur final
- [ ] Documentation relue en review
- [ ] Documentation à jour avant passage à DONE
