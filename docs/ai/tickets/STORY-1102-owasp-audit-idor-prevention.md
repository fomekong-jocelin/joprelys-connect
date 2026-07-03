# STORY-1102 — Audit OWASP & Sécurisation IDOR Portail Patient
 
> Ticket de sécurisation des accès aux DPU du portail patient.
 
## 1. Objectif
 
Auditer et renforcer les contrôles d'accès sur le portail patient (vérification systématique que l'ID du DPU demandé correspond à l'utilisateur connecté via token JWT) pour empêcher les failles IDOR (Insecure Direct Object References).
 
## 2. Critères d'acceptation
 
- [ ] Une tentative de lecture d'un autre DPU patient via l'API retourne un HTTP `403 Forbidden` ou `404 Not Found`.
- [ ] Les contrôles d'accès sont gérés au niveau du service/controller backend en comparant l'ID utilisateur extrait du jeton avec le patient lié.
- [ ] Des tests d'intégration d'intrusion simulés au vert.
 
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
| Responsable | Senior |
| Reviewer obligatoire | Tech Lead |
| Risque fonctionnel | Faible |
| Risque technique | Fort |
| Dépendances | STORY-0801 |
| Bloquants connus | Aucun |
 
## 4. Action plan
 
- [ ] Analyser les contrôles d'accès des endpoints de `/api/patient/*`.
- [ ] Ajouter les validations de correspondance d'utilisateur JWT et d'ID de patient.
- [ ] Ajouter les tests unitaires et d'intégration sécurisés.
- [ ] Mettre à jour `CHANGELOG.md` et `PROJECT-TRACKING.md`.
 
## 13. Statut final
 
Statut : TODO
 
## 14. Impact version / SemVer
 
| Champ | Valeur |
|---|---|
| Changement livrable | Oui |
| Type de bump | PATCH |
| Justification | Correctifs de sécurité rétrocompatibles |
