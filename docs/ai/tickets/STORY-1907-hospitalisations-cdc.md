# STORY-1907 — Alignement Module 10 : Hospitalisations

| Champ | Valeur |
|---|---|
| **ID** | STORY-1907 |
| **Epic** | EPIC-0014 |
| **Type** | User Story |
| **Titre** | Alignement Module 10 — Hospitalisations conformes CDC |
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

Le CDC définit un séjour hospitalier complet avec numéro de séjour, lien à une visite, médecin responsable, chambre/lit, observations journalières, actes, examens, prescriptions internes, évolution, date de sortie, résumé de sortie et document de sortie vérifiable.

---

## 2. Critères d’acceptation

### Backend

- [x] `HospitalizationEntity` contient `hospitalization_number`, `visit_id`, `responsible_practitioner_id`.
- [x] Le document de sortie est stocké dans `medical_documents` avec `document_type = FICHE_SORTIE` et `hash`.
- [x] Validation applicative d’unicité de lit (un lit ne peut être occupé que par un seul patient en cours).
- [x] L’hospitalisation apparaît dans l’historique patient et dans la synthèse médicale.

### Frontend

- [x] Formulaire d’admission avec lien à une visite et médecin responsable.
- [x] Génération/téléchargement de la fiche de sortie officielle.
- [x] Internationalisation FR/EN.

---

## 3. Tâches techniques

### Backend

1. [x] Migration V36 : ajouter `hospitalization_number`, `visit_id`, `responsible_practitioner_id`.
2. [x] Ajouter validation d'unicité dans `HospitalizationService`.
3. [x] Modifier `HospitalizationService` pour générer la fiche de sortie comme `MedicalDocumentEntity`.
4. [x] Intégrer dans `PatientSummaryService`.
5. [x] Tests unitaires et d'intégration (HospitalizationControllerTest).

### Frontend

1. [x] Modifier `patient-hospitalization.component.ts` pour les nouveaux champs.
2. [x] Ajouter la sélection de la visite et du médecin responsable.
3. [x] Tests de compilation et build.

---

## 4. Fichiers impactés

### Backend

- `hospitalization/infrastructure/persistence/HospitalizationEntity.java`
- `hospitalization/application/HospitalizationService.java`
- `hospitalization/api/HospitalizationController.java`
- `visit/infrastructure/persistence/MedicalDocumentEntity.java`
- `db/migration/V36__hospitalizations_cdc_alignment.sql`

### Frontend

- `web/src/app/patient/patient-hospitalization.component.ts`
- `web/src/app/patient/patient.models.ts`
- `web/src/app/core/i18n/i18n.service.ts`

---

## 5. Tests attendus

- [x] Backend : test d’unicité de lit (HospitalizationControllerTest).
- [x] Backend : test de génération de fiche de sortie vérifiable.
- [x] Backend : test d’intégration dans la synthèse patient.
- [x] Frontend : test du formulaire d’admission (build & compilation OK).

---

## 6. Dépendances

- STORY-1908 (Documents) pour le document de sortie vérifiable.
- STORY-1901 (Synthèse) pour l’intégration dans le PDF synthèse.

---

## 7. Risques

- Migration des hospitalisations existantes sans `visit_id`.

---

## 8. Impact version / SemVer

- Bump : **MINOR** (0.10.0).
