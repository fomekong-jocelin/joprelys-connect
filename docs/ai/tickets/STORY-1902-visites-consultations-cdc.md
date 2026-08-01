# STORY-1902 — Alignement Module 5 : Visites et consultations

| Champ | Valeur |
|---|---|
| **ID** | STORY-1902 |
| **Epic** | EPIC-0014 |
| **Type** | User Story |
| **Titre** | Alignement Module 5 — Visites et consultations conformes CDC |
| **Statut** | DONE |
| **Priorité** | P0 |
| **Stack** | Full-stack |
| **Profil recommandé** | Senior Backend + Frontend Intermédiaire |
| **Estimation Senior** | 1.5j |
| **Estimation Intermédiaire** | 2.5j |
| **Estimation Junior** | 4.0j |
| **Sprint cible** | SPRINT-0011 |
| **Assigné** | Antigravity |
| **Reviewer** | Lead Developer |
| **Dernière MAJ** | 2026-08-01 |

---

## 1. Contexte

Le CDC définit précisément les données d’une visite (`visit_number`, `patient_id`, `organization_id`, `service`, `main_practitioner_id`, `arrival_at`, `closed_at`, `reason`, `status`). Le contrat de consultation actif expose `symptoms`, `clinical_exam`, `diagnosis`, `conclusion`, `advice` et `follow_up`.

> Décision ultérieure ADR-0005 (2026-08-01) : le produit ne disposant d'aucun
> cycle de vie permettant de distinguer ces niveaux, `suspected_diagnosis` et
> `final_diagnosis` sont retirés du modèle actif par V109 au profit du seul
> `diagnosis`. V109 archive les anciennes valeurs avant suppression des colonnes.

---

## 2. Critères d’acceptation

### Backend

- [x] `VisitEntity` contient les champs `service`, `main_practitioner_id`, `arrival_at` (en plus de `created_at`).
- [x] `ConsultationEntity` contient un seul `diagnosis` et les champs distincts `conclusion`, `advice` et `follow_up`.
- [x] Les DTOs `CreateVisitRequest`, `VisitResponse`, `SaveConsultationRequest`, `ConsultationResponse` sont mis à jour.
- [x] L’échelle de douleur 0-10 est ajoutée aux constantes vitales.
- [x] FR-VISIT-005 : une visite terminée peut être corrigée uniquement via un mécanisme de trace de correction (table `visit_corrections` ou log d’audit explicite avant modification).
- [x] Les migrations sont additives.

### Frontend

- [x] Formulaire de création de visite avec `service`, `main_practitioner_id`, `arrival_at`.
- [x] Formulaire de consultation avec un seul champ `diagnosis` et une section Plan distincte.
- [x] Saisie de la douleur (0-10) dans les constantes vitales.
- [x] Internationalisation FR/EN.

---

## 3. Tâches techniques

### Backend

1. [x] Migration Flyway V31 pour les champs de visite et la douleur, puis V109 pour archiver et consolider les anciens diagnostics vers `diagnosis`.
2. [x] Mettre à jour `VisitEntity`, `ConsultationEntity`, `VitalsEntity`.
3. [x] Mettre à jour DTOs et mappers.
4. [x] Implémenter la logique de correction traçable : créer `VisitCorrectionEntity` ou log `VISIT_CORRECTION` avant toute modification d’une visite `TERMINEE`.
5. [x] Adapter `VisitService` et `ConsultationService`.
6. [x] Tests unitaires / intégration.

### Frontend

1. [x] Modifier `clinic/dashboard.component.html` pour la création de visite.
2. [x] Modifier `consultation/consultation.component.ts` pour les nouveaux champs.
3. [x] Modifier le formulaire des constantes vitales.
4. [x] Mettre à jour les modèles (`visit.models.ts`, `consultation.models.ts`).
5. [x] Tests unitaires.

---

## 4. Fichiers impactés

### Backend

- `visit/infrastructure/persistence/VisitEntity.java`
- `visit/infrastructure/persistence/VitalsEntity.java`
- `consultation/infrastructure/persistence/ConsultationEntity.java`
- `visit/api/CreateVisitRequest.java`
- `visit/api/VisitResponse.java`
- `consultation/api/SaveConsultationRequest.java`
- `consultation/api/ConsultationResponse.java`
- `visit/application/VisitService.java`
- `consultation/application/ConsultationService.java`
- `db/migration/V31__visits_consultations_cdc_alignment.sql` (nouveau)

### Frontend

- `web/src/app/clinic/dashboard.component.ts` / `.html`
- `web/src/app/consultation/consultation.component.ts`
- `web/src/app/consultation/consultation.component.html` (nouveau)
- `web/src/app/visit/visit.models.ts`
- `web/src/app/consultation/consultation.models.ts`
- `web/src/app/core/i18n/i18n.service.ts`

---

## 5. Tests attendus

- [x] Backend : test de création de visite avec tous les nouveaux champs.
- [x] Backend : test de correction d’une visite terminée avec log d’audit.
- [x] Backend : tests vérifiant le diagnostic unique et l'absence des anciens champs dans la réponse.
- [x] Frontend : test des formulaires mis à jour.
- [x] Frontend : build production OK.

---

## 6. Dépendances

- Aucune (dépend uniquement des modules existants).

---

## 7. Risques

- Modification du modèle `VisitEntity` : impact sur toutes les requêtes existantes.
- Le mécanisme de correction peut complexifier les workflows existants.

---

## 8. Impact version / SemVer

- Bump : **MINOR** (0.10.0) — ajout de champs rétrocompatibles.
