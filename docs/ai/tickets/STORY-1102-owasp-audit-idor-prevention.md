# STORY-1102 — Audit OWASP & Sécurisation IDOR Portail Patient
 
> Ticket de sécurisation des accès aux DPU du portail patient.
 
## 1. Objectif
 
Auditer et renforcer les contrôles d'accès sur le portail patient (vérification systématique que l'ID du DPU demandé correspond à l'utilisateur connecté via token JWT) pour empêcher les failles IDOR (Insecure Direct Object References).
 
## 2. Critères d'acceptation
 
- [x] Une tentative de lecture d'un autre DPU patient via l'API retourne un HTTP `403 Forbidden` ou `404 Not Found`.
- [x] Les contrôles d'accès sont gérés au niveau du service/controller backend en comparant l'ID utilisateur extrait du jeton avec le patient lié.
- [x] Des tests d'intégration d'intrusion simulés au vert.
 
## 3. Pilotage projet
 
| Champ | Valeur |
|---|---|
| Epic parent | EPIC-0008 — Portail Patient & Consentement |
| User story parent | STORY-1102 |
| Sprint cible | SPRINT-0005 |
| Priorité business | P0 |
| Complexité | L |
| Story points | 5 |
| Profil recommandé | Senior |
| Effort estimé senior | 1.1j |
| Effort estimé intermédiaire | 1.5j |
| Effort estimé junior | 2.5j |
| Responsable | Antigravity |
| Reviewer obligatoire | Tech Lead |
| Risque fonctionnel | Faible |
| Risque technique | Fort |
| Dépendances | STORY-0801 |
| Bloquants connus | Aucun |
 
## 4. Action plan

- [x] Analyser les contrôles d'accès des endpoints de `/api/patient/*`.
- [x] Créer `PatientAccessGuardService` centralisant les contrôles IDOR (OWASP A01).
- [x] Refactoriser `PatientPortalController` pour déléguer à `PatientAccessGuardService`.
- [x] Créer `PatientIdorSecurityTest` avec 4 cas de test (200, 401, 403, 404).
- [x] Créer `docs/features/story-1102/FUNCTIONAL-SPEC.md`.
- [x] Créer `docs/features/story-1102/TECHNICAL-DESIGN.md`.
- [x] Mettre à jour `CHANGELOG.md` et `PROJECT-TRACKING.md`.

## 5. Livrables

| Fichier | Type | Statut |
|---|---|---|
| `PatientAccessGuardService.java` | Service applicatif (nouveau) | ✅ Créé |
| `PatientPortalController.java` | Controller (modifié) | ✅ Refactorisé |
| `PatientIdorSecurityTest.java` | Test d'intégration (nouveau) | ✅ Créé |
| `docs/features/story-1102/FUNCTIONAL-SPEC.md` | Documentation | ✅ Créée |
| `docs/features/story-1102/TECHNICAL-DESIGN.md` | Documentation | ✅ Créée |

## 13. Statut final
 
Statut : **DONE**
 
## 14. Impact version / SemVer
 
| Champ | Valeur |
|---|---|
| Changement livrable | Oui |
| Type de bump | PATCH |
| Justification | Correctifs de sécurité rétrocompatibles |
