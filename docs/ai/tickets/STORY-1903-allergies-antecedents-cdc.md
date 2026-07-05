# STORY-1903 — Alignement Module 6 : Allergies et antécédents

| Champ | Valeur |
|---|---|
| **ID** | STORY-1903 |
| **Epic** | EPIC-0014 |
| **Type** | User Story |
| **Titre** | Alignement Module 6 — Allergies et antécédents conformes CDC |
| **Statut** | DONE |
| **Priorité** | P0 |
| **Stack** | Full-stack |
| **Profil recommandé** | Intermédiaire Full-stack |
| **Estimation Senior** | 1.0j |
| **Estimation Intermédiaire** | 1.5j |
| **Estimation Junior** | 2.5j |
| **Sprint cible** | SPRINT-0011 |
| **Assigné** | Antigravity |
| **Reviewer** | Lead Developer |
| **Dernière MAJ** | 2026-07-05 |

---

## 1. Contexte

Le CDC exige que les allergies actives apparaissent dans la synthèse médicale, que les antécédents importants soient remontés au médecin, et que chaque ajout/modification/suppression soit traçable. Les catégories doivent inclure `MEDICAL`, `SURGICAL`, `FAMILY`, `OBSTETRICAL`, `ALLERGIC`, `SOCIAL`.

---

## 2. Critères d’acceptation

### Backend

- [x] `PatientMedicalHistoryEntity` a un champ `important BOOLEAN DEFAULT FALSE`.
- [x] Les catégories d’antécédents incluent `ALLERGIC` et `SOCIAL` (enum ou validation).
- [x] Les allergies actives (`status = ACTIVE`) sont remontées dans la synthèse médicale.
- [x] Les antécédents importants sont remontés prioritairement.
- [x] Soft-delete traçable : ajout d’un champ `deleted_at` / `deleted_by` et d’un endpoint `DELETE /api/patients/{id}/medical-histories/{historyId}` qui marque comme supprimé (pas de suppression physique).
- [x] Toute modification/suppression est journalisée (`UPDATE_ALLERGY`, `DELETE_ALLERGY`, etc.).

### Frontend

- [x] Formulaire d’antécédent avec case à cocher "Important".
- [x] Liste des allergies actives visible dans la synthèse.
- [x] Option de suppression logique avec confirmation.
- [x] Internationalisation FR/EN.

---

## 3. Tâches techniques

### Backend

1. Migration V32 : ajouter `important` et `deleted_at`/`deleted_by` à `patient_medical_history` ; ajouter `deleted_at`/`deleted_by` à `patient_allergies`.
2. Mettre à jour les entités et DTOs.
3. Modifier `PatientMedicalInfoService` pour filtrer les entrées non supprimées.
4. Implémenter soft-delete dans les services/controllers.
5. Journaliser les suppressions.
6. Tests.

### Frontend

1. Modifier `patient-medical-info.component.ts` pour le flag important et la suppression.
2. Mettre à jour `patient.models.ts`.
3. Tests unitaires.

---

## 4. Fichiers impactés

### Backend

- `patient/infrastructure/persistence/PatientMedicalHistoryEntity.java`
- `patient/infrastructure/persistence/PatientAllergyEntity.java`
- `patient/application/PatientMedicalInfoService.java`
- `patient/api/PatientMedicalInfoController.java`
- `patient/api/CreatePatientMedicalHistoryRequest.java`
- `db/migration/V32__allergies_history_soft_delete_important.sql` (nouveau)

### Frontend

- `web/src/app/patient/patient-medical-info.component.ts`
- `web/src/app/patient/patient.models.ts`
- `web/src/app/core/i18n/i18n.service.ts`

---

## 5. Tests attendus

- [ ] Backend : test de soft-delete et journalisation.
- [ ] Backend : test de remontée des allergies actives dans la synthèse.
- [ ] Frontend : test de l’interface de gestion des antécédents.

---

## 6. Dépendances

- STORY-1901 pour l’intégration dans la synthèse.

---

## 7. Risques

- Impact sur les requêtes existantes si le filtre `deleted_at IS NULL` n’est pas appliqué partout.

---

## 8. Impact version / SemVer

- Bump : **MINOR** (0.10.0).
