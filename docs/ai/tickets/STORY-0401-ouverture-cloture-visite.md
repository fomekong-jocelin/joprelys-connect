# STORY-0401 — Ouverture & Clôture de Visite Patient

## 1. Objectif

Cette user story consiste à modéliser et implémenter le cycle de vie de base des visites médicales au sein de Joprelys Connect :
1. **Modèle de données** : Création de la table `visits` associée à un patient et une organisation.
2. **Génération d'identifiants** : Génération séquentielle automatique du numéro de visite (`VIS-YYYYMMDD-XXXXXX`).
3. **Cycle de vie** : Statuts `EN_COURS`, `TERMINEE`, `ANNULEE` avec horodatages d'ouverture et de clôture.
4. **API REST** : Endpoints pour ouvrir une visite, la lister (file d'attente active), et la clôturer.
5. **Interface Angular** : Formulaire d'ouverture de visite depuis le profil patient et affichage de la file d'attente active sur le tableau de bord.

## 2. Critères d'acceptation

- [x] L'agent d'accueil ou le clinicien peut ouvrir une visite pour un patient existant depuis sa fiche de détails.
- [x] Le motif de la visite (texte libre) et le service ou médecin traitant d'orientation doivent être saisis.
- [x] Le système génère automatiquement un numéro de visite séquentiel au format `VIS-YYYYMMDD-XXXXXX` (remise à zéro quotidienne globale).
- [x] Les visites sont isolées par clinique pilote (`organization_id`). Un utilisateur ne voit que les visites de sa propre clinique.
- [x] La file d'attente active sur le tableau de bord liste toutes les visites ayant le statut `EN_COURS`.
- [x] Le médecin peut clôturer une visite active (ce qui met à jour le statut à `TERMINEE` et remplit `closed_at`).
- [x] La relation JPA entre la visite et le patient est configurée en `FetchType.LAZY`.

## 3. Pilotage projet

| Champ | Valeur |
|---|---|
| Epic parent | EPIC-0004 (Gestion des Visites & Constantes Vitales) |
| User story parent | STORY-0401 |
| Sprint cible | SPRINT-0002 |
| Priorité business | P0 |
| Complexité | S |
| Story points | 2 |
| Profil recommandé | Junior autonome |
| Effort senior | 0.5j |
| Effort intermédiaire | 0.65j |
| Effort junior | 1.1j |
| Responsable | Antigravity |
| Reviewer obligatoire | Lead Developer |
| Risque fonctionnel | Moyen |
| Risque technique | Faible |
| Dépendances | STORY-0301 |
| Bloquants connus | Aucun |

## 4. Contexte analysé

- [x] `AGENTS.md` lu
- [x] `SKILL.md` lu
- [x] `PROJECT-MANAGER-SKILL.md` lu si nécessaire
- [x] `README-IA.md` lu
- [x] `WORKFLOW-IA.md` lu
- [x] `PROJECT-TRACKING.md` lu
- [x] `CHANGELOG.md` lu
- [x] `review-checklist.md` lu
- [x] Code existant analysé
- [x] Tests existants analysés
- [x] Contrats API analysés
- [x] Impacts sécurité analysés
- [x] Impacts données analysés
- [x] Impacts Angular analysés
- [x] Backend Maven uniquement vérifié
- [x] Backend `application.yml` / profils YAML vérifiés
- [x] Frontend Tailwind CSS vérifié
- [x] Absence Angular Material vérifiée
- [x] Angular `proxy.conf.json` présent et référencé dans `angular.json`
- [x] Aucun appel API Angular avec URL backend hardcodée

## 5. Hypothèses

- Un patient ne peut avoir qu'**une seule visite active (`EN_COURS`) à la fois** au sein de la clinique pour éviter les doublons accidentels d'admission.
- La file d'attente active affiche les visites triées par ordre chronologique d'arrivée (`created_at ASC`).

## 6. Risques et impacts

| Risque | Impact | Mitigation |
|---|---|---|
| Double admission accidentelle d'un patient | Faible | Blocage au niveau du service si le patient possède déjà une visite à l'état `EN_COURS`. |
| Performance lors du listage de la file d'attente | Moyen | Utilisation de `FetchType.LAZY` sur le lien de relation avec le patient et sélection uniquement des colonnes nécessaires. |

## 7. Action plan

- [x] **Specs & Documentation** : Rédiger la spécification fonctionnelle et technique dans `docs/features/visite/`.
- [x] **DB/Migration** (TASK-0401-01) : Créer la table `visits` via Flyway.
- [x] **Backend/API** (TASK-0401-02) : Créer l'entité, le repository, le service et les controllers de Visite.
- [x] **Frontend/API** : Créer le modèle visite et le service `VisitApiService`.
- [x] **Frontend/UI** (TASK-0401-03 & TASK-0401-04) : Créer le formulaire d'ouverture de visite sur le profil patient et le tableau de file d'attente.
- [x] **Tests/QA** (TASK-0401-05) : Écrire les tests d'intégration backend et les tests unitaires frontend.
- [x] Mettre à jour `CHANGELOG.md` et `PROJECT-TRACKING.md`.

## 8. Implémentation réalisée

* **Base de données** : Script Flyway `V4__create_visits_table.sql` définissant la structure, contraintes de clés étrangères et indexes de recherche.
* **Backend Spring Boot** :
  * Entité `VisitEntity.java` associée à `@TenantId` d'Hibernate et à la relation LAZY vers le patient.
  * `VisitRepository.java` contenant la requête `findActiveVisits()` optimisée par JOIN FETCH, et la détection d'unicité `existsByPatientIdAndStatus()`.
  * Générateur séquentiel de numéro `VisitNumberGenerator.java` (`VIS-YYYYMMDD-XXXXXX`).
  * `VisitService.java` de logique métier (admission, file d'attente, clôture).
  * `VisitController.java` exposant les routes sécurisées `/api/visits` avec habilitation des rôles cliniques.
  * Gestionnaire d'exceptions global : Intégration de `ResponseStatusException` dans `AuthExceptionHandler.java` pour renvoyer des messages formatés en `ProblemDetail` contenant le champ `"detail"`.
* **Frontend Angular** :
  * Service `VisitApiService.ts` et interfaces de types.
  * Intégration de la boîte de dialogue d'admission (modal respectant la charte graphique avec select/textarea `.ui-select` et `.ui-textarea` adaptés au thème light/dark) sur `PatientDetailComponent.ts` et gestion fine des erreurs d'API propagées.
  * Tableau et cartes réactives (mobile-first) de file d'attente active des patients sur `DashboardComponent` (html/ts).

## 9. Suivi d'exécution

| Date | Développeur | Temps passé | Avancement | Reste à faire | Blocage | Commentaire |
|---|---|---:|---:|---:|---|---|
| 2026-07-02 | Antigravity | 0.5j | 100% | Aucun | Aucun | Développement et tests validés à 100% |

## 10. Tests et vérifications

* **Tests Backend** : `VisitControllerTest.java` (5 tests d'intégration complets validant la création, les conflits de double admission, l'isolation multi-tenant stricte, la clôture autorisée aux médecins et interdite aux secrétaires). Exécution de `.\mvnw.cmd test` -> **BUILD SUCCESS** (28/28 tests OK).
* **Tests Frontend** : `dashboard.component.spec.ts` validant le chargement automatique de la file d'attente. Exécution de `npm test` -> **14/14 tests OK**.
* **Compilation et Build Production** : `npm run build` exécuté avec succès (0 erreurs).

## 11. Documentation

- [x] Spécification fonctionnelle créée : `docs/features/visite/FUNCTIONAL-SPEC.md`
- [x] Spécification technique créée : `docs/features/visite/TECHNICAL-DESIGN.md`

## 12. Reste à faire

* Aucun (Story terminée).

## 13. Statut final

Statut : REVIEW

## 14. Impact version / SemVer

| Champ | Valeur |
|---|---|
| Changement livrable | Oui |
| Type de bump | MINOR |
| Justification | Ajout de la gestion des visites patients et de la file d'attente active |
| Breaking change | Non |
| Migration DB | Oui |
| Changement API | Oui |
| Impact Angular | Oui |
| Impact Flutter | Non |
| Changelog requis | Oui |
| Release note requise | Oui |
