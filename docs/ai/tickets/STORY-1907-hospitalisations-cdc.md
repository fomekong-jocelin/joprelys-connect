# STORY-1907 — Alignement Module 10 : Hospitalisations

| Champ | Valeur |
|---|---|
| **ID** | STORY-1907 |
| **Epic** | EPIC-0014 |
| **Type** | User Story |
| **Titre** | Alignement Module 10 — Hospitalisations conformes CDC |
| **Statut** | READY |
| **Priorité** | P0 |
| **Stack** | Full-stack |
| **Profil recommandé** | Intermédiaire Full-stack |
| **Estimation Senior** | 1.0j |
| **Estimation Intermédiaire** | 1.5j |
| **Estimation Junior** | 2.5j |
| **Sprint cible** | SPRINT-0011 |
| **Assigné** | À assigner |
| **Reviewer** | Lead Developer |
| **Dernière MAJ** | 2026-07-05 |

---

## 1. Contexte

Le CDC définit un séjour hospitalier complet avec numéro de séjour, lien à une visite, médecin responsable, chambre/lit, observations journalières, actes, examens, prescriptions internes, évolution, date de sortie, résumé de sortie et document de sortie vérifiable.

---

## 2. Critères d’acceptation

### Backend

- [ ] `HospitalizationEntity` contient `hospitalization_number`, `visit_id`, `responsible_practitioner_id`.
- [ ] Tables filles pour les actes, examens et prescriptions internes du séjour (ou réutilisation des entités existantes avec un lien `hospitalization_id`).
- [ ] Le document de sortie est stocké dans `medical_documents` avec `document_type = FICHE_SORTIE` et `hash`.
- [ ] Contrainte DB d’unicité partielle : un lit ne peut être occupé que par un seul patient en cours (`status = EN_COURS`).
- [ ] L’hospitalisation apparaît dans l’historique patient et dans la synthèse médicale.

### Frontend

- [ ] Formulaire d’admission avec lien à une visite et médecin responsable.
- [ ] Interface de gestion des actes, examens et prescriptions internes.
- [ ] Génération/téléchargement de la fiche de sortie vérifiable.
- [ ] Internationalisation FR/EN.

---

## 3. Tâches techniques

### Backend

1. Migration V36 : ajouter `hospitalization_number`, `visit_id`, `responsible_practitioner_id` ; créer tables filles ou liens.
2. Ajouter contrainte d’unicité partielle sur `(room_number, bed_number, status)`.
3. Modifier `HospitalizationService` pour générer la fiche de sortie comme `MedicalDocumentEntity`.
4. Intégrer dans `PatientSummaryService`.
5. Tests.

### Frontend

1. Modifier `patient-hospitalization.component.ts` pour les nouveaux champs.
2. Modifier `patient-hospitalizations-tab.component.ts`.
3. Ajouter la gestion des actes/examens/prescriptions internes.
4. Tests.

---

## 4. Fichiers impactés

### Backend

- `hospitalization/infrastructure/persistence/HospitalizationEntity.java`
- `hospitalization/application/HospitalizationService.java`
- `hospitalization/api/HospitalizationController.java`
- `visit/infrastructure/persistence/MedicalDocumentEntity.java`
- `db/migration/V36__hospitalizations_cdc_alignment.sql` (nouveau)

### Frontend

- `web/src/app/patient/patient-hospitalization.component.ts`
- `web/src/app/patient/detail/patient-hospitalizations-tab.component.ts`
- `web/src/app/patient/patient.models.ts`
- `web/src/app/core/i18n/i18n.service.ts`

---

## 5. Tests attendus

- [ ] Backend : test d’unicité de lit.
- [ ] Backend : test de génération de fiche de sortie vérifiable.
- [ ] Backend : test d’intégration dans la synthèse patient.
- [ ] Frontend : test du formulaire d’admission.

---

## 6. Dépendances

- STORY-1908 (Documents) pour le document de sortie vérifiable.
- STORY-1901 (Synthèse) pour l’intégration dans le PDF synthèse.

---

## 7. Risques

- Migration des hospitalisations existantes sans `visit_id`.
- Complexité des tables filles (actes/examens/prescriptions internes).

---

## 8. Impact version / SemVer

- Bump : **MINOR** (0.10.0).
