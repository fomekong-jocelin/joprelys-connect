# STORY-1105 — Nettoyage de la dette technique & Application de la checklist QA
 
> Ticket d'audit et d'assurance qualité du code et de la documentation.
 
## 1. Objectif
 
Supprimer les dépendances Maven test invalides (artefacts `spring-boot-starter-*-test` inexistants dans Maven Central pour Spring Boot 4.1), ajouter `spring-security-test` correct, compléter le profil de test H2, et activer `repair-on-migrate` Flyway.
 
## 2. Critères d'acceptation
 
- [x] Aucun artefact Maven invalide dans `pom.xml` — suppression de 5 dépendances `spring-boot-starter-*-test` inexistantes.
- [x] `spring-security-test` (org.springframework.security) ajouté correctement en scope test.
- [x] `repair-on-migrate: true` ajouté sous `flyway:` dans `application.yml`.
- [x] `application-test.yml` complet avec H2, JWT test, Flyway, seed désactivé et propriétés métier.
- [x] Documentation dans `docs/features/story-1105/` créée (FUNCTIONAL-SPEC.md + TECHNICAL-DESIGN.md).
- [x] `.gitignore` vérifié — `target/`, `.env`, `*.log` présents, fichiers de gouvernance IA non ignorés.
 
## 3. Pilotage projet
 
| Champ | Valeur |
|---|---|
| Epic parent | EPIC-0012 — Assurance Qualité & Documentation |
| User story parent | STORY-1105 |
| Sprint cible | SPRINT-0005 |
| Priorité business | P2 |
| Complexité | S |
| Story points | 2 |
| Profil recommandé | Junior |
| Effort estimé senior | 0.4j |
| Effort estimé intermédiaire | 0.5j |
| Effort estimé junior | 0.8j |
| Responsable | Antigravity |
| Reviewer obligatoire | Tech Lead |
| Risque fonctionnel | Faible |
| Risque technique | Faible |
| Dépendances | Aucune |
| Bloquants connus | Aucun |
 
## 4. Action plan

- [x] Supprimer les 5 dépendances test invalides de `pom.xml`.
- [x] Ajouter `spring-security-test` (org.springframework.security) en scope test.
- [x] Ajouter `repair-on-migrate: true` dans `application.yml`.
- [x] Créer / mettre à jour `src/test/resources/application-test.yml` complet.
- [x] Vérifier `.gitignore` (target/, .env, *.log présents).
- [x] Créer `docs/features/story-1105/FUNCTIONAL-SPEC.md`.
- [x] Créer `docs/features/story-1105/TECHNICAL-DESIGN.md`.
- [x] Mettre à jour `CHANGELOG.md` et `PROJECT-TRACKING.md`.
 
## 13. Statut final
 
Statut : DONE — 2026-07-03
 
## 14. Impact version / SemVer
 
| Champ | Valeur |
|---|---|
| Changement livrable | Non |
| Type de bump | Aucun |
| Justification | Nettoyage de code et documentation interne |

