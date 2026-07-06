# STORY-1906 — Alignement Module 9 : Résultats d’examens

| Champ | Valeur |
|---|---|
| **ID** | STORY-1906 |
| **Epic** | EPIC-0014 |
| **Type** | User Story |
| **Titre** | Alignement Module 9 — Résultats d’examens conformes CDC |
| **Statut** | DONE |
| **Priorité** | P0 |
| **Stack** | Full-stack |
| **Profil recommandé** | Senior Backend |
| **Estimation Senior** | 1.5j |
| **Estimation Intermédiaire** | 2.5j |
| **Estimation Junior** | 4.0j |
| **Sprint cible** | SPRINT-0011 |
| **Assigné** | Antigravity |
| **Reviewer** | Lead Developer |
| **Dernière MAJ** | 2026-07-05 |

---

## 1. Contexte

Le CDC exige que les résultats d’examens aient un cycle de vie strict (brouillon / validé / annulé), soient immuables une fois validés, disposent d’un validateur identifié, d’une conclusion, d’un document vérifiable, et soient exportables / mappables FHIR.

---

## 2. Critères d’acceptation

### Backend

- [x] `LabResultEntity` contient `status` (`DRAFT`, `VALIDATED`, `CANCELLED`).
- [x] `validator_user_id` remplace `validator_name` (UUID lié à `users`).
- [x] `conclusion` est persisté.
- [x] `document_id` lie le résultat à `medical_documents`.
- [x] FR-RESULT-001 : un résultat `VALIDATED` ne peut plus être modifié ; toute modification génère une nouvelle version (ou nouvelle entité liée).
- [x] FR-RESULT-004 : endpoint d’export structuré (`GET /api/patients/{id}/exam-results/export?format=csv|json`).
- [x] FR-RESULT-005 : endpoint FHIR `GET /fhir/DiagnosticReport?patient={id}` et `GET /fhir/Observation?patient={id}`.
- [x] `result_number` généré via séquence DB ou UUID sûr en concurrence.

### Frontend

- [x] Page "Mes résultats" dans le portail patient (`/patient/results`).
- [x] Écran de validation de résultat dans le portail labo.
- [x] Affichage de la conclusion et du statut de validation.
- [x] Internationalisation FR/EN.

---

## 3. Tâches techniques

### Backend

1. Migration V35 : ajouter `status`, `validator_user_id`, `conclusion`, `document_id`, `version`/`parent_result_id` à `lab_results`.
2. Mettre à jour `LabResultEntity`.
3. Modifier `LabResultService` pour gérer le statut et l’immutabilité.
4. Créer endpoints d’export et FHIR.
5. Générer `result_number` via séquence DB.
6. Tests.

### Frontend

1. Créer `PatientResultsPageComponent`.
2. Modifier `lab-orders-page.component.ts` pour la validation.
3. Mettre à jour modèles et API service.
4. Tests.

---

## 4. Fichiers impactés

### Backend

- `lab/infrastructure/persistence/LabResultEntity.java`
- `lab/application/LabResultService.java`
- `lab/api/LabResultUploadController.java`
- `fhir/api/FhirController.java`
- `fhir/application/FhirService.java`
- `fhir/FhirDiagnosticReportMapper.java` (nouveau)
- `db/migration/V35__lab_results_cdc_alignment.sql` (nouveau)

### Frontend

- `web/src/app/patient/portal/pages/patient-results-page.component.ts` (nouveau)
- `web/src/app/clinic/lab/lab-orders-page.component.ts`
- `web/src/app/clinic/lab/lab.models.ts`
- `web/src/app/patient/portal/patient-dashboard.component.ts`
- `web/src/app/app.routes.ts`
- `web/src/app/core/i18n/i18n.service.ts`

---

## 5. Tests attendus

- [x] Backend : test d’immutabilité d’un résultat validé.
- [x] Backend : test d’export CSV/JSON.
- [x] Backend : test FHIR DiagnosticReport/Observation.
- [x] Frontend : test de la page résultats patient.

---

## 6. Dépendances

- STORY-1908 (Documents médicaux) pour `document_id`.
- STORY-1905 (Examens) pour le lien demande/résultat.

---

## 7. Risques

- Migration des résultats existants vers le nouveau modèle.
- Gestion du versioning : choix entre nouvelle entité ou champ `version`.

---

## 8. Impact version / SemVer

- Bump : **MINOR** (0.10.0).
