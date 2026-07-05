# STORY-1901 — Alignement Module 4 : Dossier patient partagé et synthèse médicale

| Champ | Valeur |
|---|---|
| **ID** | STORY-1901 |
| **Epic** | EPIC-0014 |
| **Type** | User Story |
| **Titre** | Alignement Module 4 — Dossier patient partagé et synthèse médicale conforme CDC |
| **Statut** | DONE |
| **Priorité** | P0 |
| **Stack** | Full-stack |
| **Profil recommandé** | Senior Full-stack |
| **Estimation Senior** | 2.0j |
| **Estimation Intermédiaire** | 3.0j |
| **Estimation Junior** | 5.0j |
| **Sprint cible** | SPRINT-0011 |
| **Assigné** | Antigravity |
| **Reviewer** | Lead Developer |
| **Dernière MAJ** | 2026-07-05 |

---

## 1. Contexte

Le CDC exige un Dossier Patient Unique (DPU) centralisant 20 sections médicales et une **synthèse médicale rapide** contenant les éléments critiques de prise en charge. Actuellement, la synthèse PDF s’appuie sur des champs texte libre (`PatientEntity.allergies`, `medicalHistory`) et ignore les données structurées (allergies actives, antécédents importants, diagnostics, prescriptions, résultats critiques).

---

## 2. Critères d’acceptation

### Backend

- [x] La synthèse médicale PDF (`GET /api/patients/{id}/summary-pdf`) intègre :
  - identité minimale (nom, DPU, date naissance, groupe sanguin) ;
  - allergies actives structurées (`patient_allergies.status = ACTIVE`) ;
  - antécédents marqués comme importants ou en cours (`patient_medical_history.is_ongoing = true` ou nouveau flag `important = true`) ;
  - maladies chroniques (antécédents en cours ou entité dédiée) ;
  - traitements en cours (prescriptions `ACTIVE`) ;
  - dernières visites (3 dernières) ;
  - derniers diagnostics (3 derniers) ;
  - dernières prescriptions actives ;
  - derniers résultats critiques (`interpretation = CRITICAL`).
- [x] Synchronisation des champs texte libre `PatientEntity.allergies` et `medicalHistory` avec les tables structurées.
- [x] Création d’un endpoint `GET /api/patients/{id}/medical-summary` retournant la synthèse structurée (JSON) pour le frontend.
- [x] Les accès à la synthèse sont journalisés (`READ_PATIENT_SUMMARY`).

### Frontend

- [x] Création de la page **"Ma synthèse médicale"** dans le portail patient (`/patient/summary`).
- [x] Affichage de la synthèse sous forme de cartes lisibles avec icônes de gravité (allergies, résultats critiques).
- [x] Internationalisation FR/EN complète.
- [x] Design system Tailwind v4, composants partagés, arrondis sobres.

---

## 3. Tâches techniques

### Backend

1. [x] Migration Flyway additive : ajouter `important BOOLEAN DEFAULT FALSE` à `patient_medical_history`.
2. [x] Modifier `PatientMedicalHistoryEntity` avec le nouveau champ.
3. [x] Créer un `PatientSummaryService` dédié (ne pas alourdir `PatientService`).
4. [x] Refondre `PdfGeneratorService.generatePatientSummaryPdf()` pour utiliser les tables structurées.
5. [x] Créer `MedicalSummaryResponse` DTO et endpoint `GET /api/patients/{id}/medical-summary`.
6. [x] Journaliser les accès via `AuditService`.
7. [x] Tests unitaires et d’intégration MockMvc.

### Frontend

1. [x] Créer `PatientSummaryPageComponent`.
2. [x] Ajouter la route `/patient/summary`.
3. [x] Ajouter le lien dans la sidebar du portail patient.
4. [x] Créer/service `patient-api.service.getMedicalSummary(patientId)`.
5. [x] Utiliser `<app-ui-card>`, `<app-status-badge>`, `<app-page-header>`.
6. [x] Tests unitaires Vitest.

---

## 4. Fichiers impactés

### Backend

- `patient/application/PatientService.java`
- `patient/application/PatientMedicalInfoService.java`
- `visit/application/PdfGeneratorService.java`
- `patient/api/PatientController.java`
- `patient/infrastructure/persistence/PatientMedicalHistoryEntity.java`
- `db/migration/V30__medical_history_important_flag.sql` (nouvelle)

### Frontend

- `web/src/app/patient/portal/pages/patient-summary-page.component.ts` (nouveau)
- `web/src/app/patient/portal/patient-dashboard.component.ts`
- `web/src/app/patient/patient-api.service.ts`
- `web/src/app/app.routes.ts`
- `web/src/app/shared/layout/app-shell.component.ts`
- `web/src/app/core/i18n/i18n.service.ts`

---

## 5. Tests attendus

- [x] Backend : test de génération PDF synthèse avec allergies actives et résultats critiques.
- [x] Backend : test du endpoint `/api/patients/{id}/medical-summary` avec contrôle IDOR.
- [x] Backend : test d’audit `READ_PATIENT_SUMMARY`.
- [x] Frontend : test du composant `PatientSummaryPageComponent`.
- [x] Frontend : test de build production (`npm run build`).

---

## 6. Dépendances

- STORY-1903 (Allergies / antécédents) pour le flag `important`.
- STORY-1904 (Prescriptions) pour les traitements en cours.
- STORY-1906 (Résultats) pour les résultats critiques.

---

## 7. Risques

- Risque de régression sur le PDF synthèse existant (tests obligatoires).
- Suppression des champs texte libre : vérifier qu’aucun écran ne les utilise encore.

---

## 8. Impact version / SemVer

- Bump : **MINOR** (0.10.0) — nouvelle fonctionnalité rétrocompatible.
- Pas de breaking change si les champs texte libre sont conservés en read-only temporairement.
