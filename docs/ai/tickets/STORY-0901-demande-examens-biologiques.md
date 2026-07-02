# TICKET-STORY-0901 — Demande d'examens biologiques (médecin)

## 1. Objectif

Permettre au médecin, lors d'une consultation, de rédiger une demande d'examens biologiques pour le patient. Les examens sélectionnés font partie d'un catalogue standard (ex: NFS, Glycémie, Bilan lipidique, Bilan rénal) et sont rattachés au patient sous le statut `REQUESTED` avec un identifiant unique `exam_request_number`.

---

## 2. Critères d'acceptation

- [ ] **Modèle de données** : Création de la table `lab_orders` pour modéliser les demandes d'examens biologiques avec génération d'un numéro unique `EXAM-REQ-YYYYMMDD-XXXXXX`.
- [ ] **Endpoints REST Backend** :
  - `POST /api/lab-orders` : Permet au médecin de soumettre une demande structurée.
  - `GET /api/lab-orders/patient/{patientId}` : Permet de lister toutes les demandes de laboratoire d'un patient.
- [ ] **IHM Médecin (Angular)** :
  - Onglet de prescription d'examens biologiques dans le formulaire de consultation médicale.
  - Sélection multi-critères des examens dans une liste de marqueurs standards.
  - Génération visuelle du bon de demande avec code-barres ou numéro de demande.

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
- [ ] Code existant analysé
- [ ] Tests existants analysés
- [ ] Contrats API analysés
- [x] Impacts backend analysés (Spring Boot avec Maven uniquement, configuration YAML)
- [x] Impacts Angular analysés (Tailwind CSS v4 CSS-first, sans Material, proxy config respecté)

---

## 5. Hypothèses

- Les marqueurs d'examens standards seront initialement stockés sous forme d'énumération ou de configuration statique simple pour le MVP.
- La structure de la demande d'examens sera rattachée à la table `patients` et facultativement à la table `visits` (motif de la visite).

---

## 6. Risques et impacts

| Risque | Impact | Mitigation |
|---|---|---|
| Génération de doublons de numéros d'examens | Faible | Utilisation d'une séquence PostgreSQL ou d'une clé de hachage unique temporelle. |

---

## 7. Action plan

- [ ] **Backend** :
  - [ ] Créer l'entité `LabOrderEntity` et son repository.
  - [ ] Implémenter le service métier `LabOrderService`.
  - [ ] Implémenter le controller `LabOrderController` sous `/api/lab-orders`.
  - [ ] Écrire les tests unitaires et d'intégration Spring Boot.
- [ ] **Frontend** :
  - [ ] Créer le modèle TypeScript `LabOrder`.
  - [ ] Créer le service Angular `LabOrderApiService`.
  - [ ] Intégrer la sélection d'examens dans `ConsultationComponent` (formulaire médecin).
  - [ ] Ajouter les tests unitaires frontend associés.
- [ ] Mettre à jour `CHANGELOG.md` et `PROJECT-TRACKING.md`.

---

## 8. Implémentation réalisée

*(En cours d'initialisation)*

---

## 9. Suivi d'exécution

| Date | Développeur | Temps passé | Avancement | Reste à faire | Blocage | Commentaire |
|---|---|---:|---:|---:|---|---|
| 2026-07-03 | Antigravity | 0.02j | 5% | Tout | Aucun | Scaffolding du ticket et initialisation du backlog |

---

## 10. Tests et vérifications

*(En attente d'exécution)*

---

## 11. Documentation

- [x] Documentation fonctionnelle initiale créée / mise à jour : `docs/features/lab-integration/FUNCTIONAL-SPEC.md`
- [x] Documentation technique initiale créée / mise à jour : `docs/features/lab-integration/TECHNICAL-DESIGN.md`

---

## 12. Reste à faire

- [ ] Initialisation du code backend (Entity, Repository, Controller).
- [ ] Intégration de l'IHM médecin.

---

## 13. Statut final

Statut : **IN_PROGRESS**

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
