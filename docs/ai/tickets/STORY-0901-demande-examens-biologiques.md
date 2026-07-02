# TICKET-STORY-0901 — Demande d'examens biologiques (médecin)

## 1. Objectif

Permettre au médecin, lors d'une consultation, de rédiger une demande d'examens biologiques pour le patient. Les examens sélectionnés font partie d'un catalogue standard (ex: NFS, Glycémie, Bilan lipidique, Bilan rénal) et sont rattachés au patient sous le statut `REQUESTED` avec un identifiant unique `exam_request_number`.

---

## 2. Critères d'acceptation

- [x] **Modèle de données** : Création de la table `lab_orders` pour modéliser les demandes d'examens biologiques avec génération d'un numéro unique `EXAM-REQ-YYYYMMDD-XXXXXX`.
- [x] **Endpoints REST Backend** :
  - `POST /api/lab-orders` : Permet au médecin de soumettre une demande structurée.
  - `GET /api/lab-orders/patient/{patientId}` : Permet de lister toutes les demandes de laboratoire d'un patient.
- [x] **IHM Médecin (Angular)** :
  - Onglet de prescription d'examens biologiques dans le formulaire de consultation médicale.
  - Sélection multi-critères des examens dans une liste de marqueurs standards.
  - Enregistrement automatique lors de la sauvegarde de la consultation.

---

## 3. Pilotage projet

| Champ | Valeur |
|---|---|
| Epic parent | `EPIC-0009` — Intégration Laboratoire & Examens Biologiques |
| User story parent | `STORY-0901` |
| Sprint cible | SPRINT-0004 |
| Priorité business | P1 |
| Complexité | M |
| Story points | 1.5 SP |
| Profil recommandé | Intermédiaire |
| Effort estimé senior | 0.25j |
| Effort estimé intermédiaire | 0.4j |
| Effort estimé junior | 0.8j |
| Responsable | Antigravity |
| Reviewer obligatoire | Lead |
| Risque fonctionnel | Faible |
| Risque technique | Moyen |
| Dépendances | Aucun |
| Bloquants connus | Aucun |

---

## 4. Contexte analysé

- [x] `AGENTS.md` lu
- [x] `SKILL.md` lu
- [x] `README-IA.md` lu
- [x] `WORKFLOW-IA.md` lu
- [x] `PROJECT-TRACKING.md` lu
- [x] `CHANGELOG.md` lu
- [x] `review-checklist.md` lu
- [x] Code existant analysé
- [x] Tests existants analysés
- [x] Contrats API analysés
- [x] Impacts backend analysés (Spring Boot avec Maven uniquement, configuration YAML)
- [x] Impacts Angular analysés (Tailwind CSS v4 CSS-first, sans Material, proxy config respecté)

---

## 5. Hypothèses

- Les marqueurs d'examens standards sont stockés sous forme d'une liste statique côté frontend pour l'IHM et persistés en chaîne délimitée par des virgules dans la base de données.
- Le tenant ID (`organization_id`) est résolu à partir de l'utilisateur connecté via JWT de manière sécurisée.

---

## 6. Risques et impacts

| Risque | Impact | Mitigation |
|---|---|---|
| Génération de doublons de numéros d'examens | Faible | Utilisation d'un compteur séquentiel quotidien formaté à l'échelle de l'application. |

---

## 7. Action plan

- [x] **Backend** :
  - [x] Créer l'entité `LabOrderEntity` et son repository.
  - [x] Implémenter le service métier `LabOrderService`.
  - [x] Implémenter le controller `LabOrderController` sous `/api/lab-orders`.
  - [x] Écrire les tests unitaires et d'intégration Spring Boot.
- [x] **Frontend** :
  - [x] Créer le modèle TypeScript `LabOrder`.
  - [x] Créer le service Angular `LabOrderApiService`.
  - [x] Intégrer la sélection d'examens dans `ConsultationComponent` (formulaire médecin).
- [x] Mettre à jour `CHANGELOG.md` et `PROJECT-TRACKING.md`.

---

## 8. Implémentation réalisée

- **Migration SQL Flyway** (`V12__create_lab_orders_table.sql`) pour la table `lab_orders`.
- **Entité JPA `LabOrderEntity`** mappée avec `@TenantId` pour l'isolation multi-tenant stricte.
- **Repository Spring Data `LabOrderRepository`** avec résolution automatique par patient.
- **Service Applicatif `LabOrderService`** : génération du numéro séquentiel unique quotidien `EXAM-REQ-YYYYMMDD-XXXXXX` et persistance.
- **Controller REST `LabOrderController`** sécurisé avec `@PreAuthorize`.
- **Modèle TypeScript** `lab.models.ts` et client API Angular `lab-api.service.ts`.
- **Intégration IHM** : Ajout du bloc interactif "Demande d'Examens Biologiques" dans le formulaire de consultation médecin, avec suggestions rapides d'analyses courantes, ajout personnalisé, gestion de la priorité et des indications cliniques.

---

## 9. Suivi d'exécution

| Date | Développeur | Temps passé | Avancement | Reste à faire | Blocage | Commentaire |
|---|---|---:|---:|---:|---|---|
| 2026-07-03 | Antigravity | 0.4j | 100% | Aucun | Aucun | Développement backend & frontend terminé et validé |

---

## 10. Tests et vérifications

- [x] Tests backend intégrés : `LabOrderControllerTest.java` (couverture création, interdiction rôles, isolation multi-tenant).
- [x] Exécution de `./mvnw test` : Succès total (`107/107` tests au vert).
- [x] Build frontend : compilation TypeScript et Angular vérifiée par `npm run build` (génération réussie du bundle).

---

## 11. Documentation

- [x] Documentation fonctionnelle initiale créée / mise à jour : `docs/features/lab-integration/FUNCTIONAL-SPEC.md`
- [x] Documentation technique initiale créée / mise à jour : `docs/features/lab-integration/TECHNICAL-DESIGN.md`

---

## 12. Reste à faire

- Aucun. La story est prête pour la validation en environnement de test.

---

## 13. Statut final

Statut : **DONE**

---

## 14. Impact version / SemVer

| Champ | Valeur |
|---|---|
| Changement livrable | Oui |
| Type de bump | MINOR |
| Justification | Ajout de la fonctionnalité de demande d'examens structurés. |
| Breaking change | Non |
| Migration DB | Oui |
| Changement API | Oui |
| Impact Angular | Oui |
| Impact Flutter | Non |
| Changelog requis | Oui |
| Release note requise | Oui |
