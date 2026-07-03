# STORY-1003 — Traçabilité & Statut ordonnance

> Fichier obligatoire pour le ticket de la User Story STORY-1003.

## 1. Objectif

Permettre aux médecins et aux patients de suivre l'état de délivrance de l'ordonnance en temps réel et de visualiser l'historique des dispensations.

## 2. Critères d'acceptation

- [ ] **IHM Médecin (Clinique)** :
  - Sur le DPU du patient, dans l'historique médical (liste des prescriptions/consultations), afficher le statut de l'ordonnance (`ACTIVE`, `PARTIALLY_DISPENSED`, `FULLY_DISPENSED`, `CANCELLED`).
  - Permettre au médecin de cliquer pour déplier le détail des dispensations réalisées (date, nom de la pharmacie, médicaments délivrés).
- [ ] **IHM Patient (Portail Patient)** :
  - Sur le portail patient, afficher également le statut de délivrance en temps réel de ses ordonnances.
- [ ] **Sécurité (Vérification QR Code)** :
  - Si le QR code est scanné pour vérification publique, afficher le statut actuel (ex : "Entièrement servie" ou "Partiellement servie") pour assurer une transparence complète.

## 3. Pilotage projet

| Champ | Valeur |
|---|---|
| Epic parent | EPIC-0010 — Dispensation en Pharmacie & Gestion des Prescriptions |
| User story parent | STORY-1003 |
| Sprint cible | SPRINT-0004 |
| Priorité business | P1 |
| Complexité | M |
| Story points | 2 |
| Profil recommandé | Intermédiaire |
| Effort estimé senior | 0.3j |
| Effort estimé intermédiaire | 0.5j |
| Effort estimé junior | 0.8j |
| Responsable | Antigravity |
| Reviewer obligatoire | Lead |
| Risque fonctionnel | Faible |
| Risque technique | Faible |
| Dépendances | STORY-1001, STORY-1002 |
| Bloquants connus | Aucun |

## 4. Contexte analysé

- [x] `AGENTS.md` lu
- [x] `SKILL.md` lu
- [x] `PROJECT-TRACKING.md` lu
- [x] `CHANGELOG.md` lu

## 5. Hypothèses

- L'affichage se fait au sein de l'accordéon existant du dossier médical du patient.

## 6. Risques et impacts

| Risque | Impact | Mitigation |
|---|---|---|
| Incohérences d'affichage en direct | Faible | Rechargement automatique de l'ordonnance. |

## 7. Action plan

- [ ] **Modèle backend** : Exposer les informations de dispensation dans `PrescriptionResponse` et `PatientPortalMeResponse.PatientPortalPrescription`.
- [ ] **Frontend Angular Clinique** : Mettre à jour `PatientDetailComponent` pour afficher le statut et le détail des dispensations de l'ordonnance.
- [ ] **Frontend Angular Patient** : Mettre à jour `PatientDashboardComponent` / `PatientVisitsListComponent` pour refléter l'état de délivrance.
- [ ] **Page de vérification publique** : Afficher l'état de dispensation sur `VerificationComponent`.
- [ ] **Tests** :
  - Écrire des tests unitaires Angular vérifiant le bon rendu du statut et de l'historique de délivrance.
- [ ] Mettre à jour `PROJECT-TRACKING.md` et `CHANGELOG.md`.

## 8. Implémentation réalisée

*(À remplir lors de la réalisation)*

## 9. Suivi d'exécution

*(À remplir lors de la réalisation)*

## 10. Tests et vérifications

*(À remplir lors de la réalisation)*

## 11. Documentation

*(À remplir lors de la réalisation)*

## 12. Reste à faire

*(À remplir lors de la réalisation)*

## 13. Statut final

Statut : TODO

## 14. Impact version / SemVer

| Champ | Valeur |
|---|---|
| Changement livrable | Oui |
| Type de bump | PATCH |
| Justification | Améliorations de l'IHM et intégration du statut de dispensation de l'ordonnance. |
| Breaking change | Non |
| Migration DB | Non |
| Changement API | Oui |
| Impact Angular | Oui |
| Impact Flutter | Non |
| Changelog requis | Oui |
| Release note requise | Oui |
