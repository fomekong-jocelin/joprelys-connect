# STORY-1104 — Import de résultats structurés de laboratoire (FHIR/HL7)
 
> Ticket d'intégration structurée de résultats d'analyses médicales.
 
## 1. Objectif
 
Implémenter un parseur de fichiers structurés (JSON/XML au format FHIR DiagnosticReport) pour ingérer les résultats d'analyses et mettre à jour automatiquement les constantes du DPU du patient.
 
## 2. Critères d'acceptation
 
- [x] L'API publique d'upload supporte le format FHIR standard DiagnosticReport en entrée.
- [x] Les valeurs d'analyses (glycémie, cholestérol, etc.) parsées alimentent directement l'historique des constantes vitales du patient lié.
- [x] Les erreurs de structure ou de format de données sont gérées gracieusement (HTTP 400 avec détails).
 
## 3. Pilotage projet
 
| Champ | Valeur |
|---|---|
| Epic parent | EPIC-0009 — Intégration Laboratoire & Examens Biologiques |
| User story parent | STORY-1104 |
| Sprint cible | SPRINT-0005 |
| Priorité business | P1 |
| Complexité | L |
| Story points | 5 |
| Profil recommandé | Senior |
| Effort estimé senior | 1.2j |
| Effort estimé intermédiaire | 1.6j |
| Effort estimé junior | 2.8j |
| Responsable | Senior |
| Reviewer obligatoire | Tech Lead |
| Risque fonctionnel | Moyen |
| Risque technique | Fort |
| Dépendances | STORY-0902 |
| Bloquants connus | Aucun |
 
## 4. Action plan
 
- [x] Créer le parseur FHIR DiagnosticReport JSON/XML dans le backend.
- [x] Ajouter les liaisons de constantes vitales et historiques du patient.
- [x] Écrire les tests unitaires avec des payloads de test FHIR valides et invalides.
- [x] Mettre à jour `CHANGELOG.md` et `PROJECT-TRACKING.md`.
 
## 13. Statut final
 
Statut : DONE
 
## 14. Impact version / SemVer
 
| Champ | Valeur |
|---|---|
| Changement livrable | Oui |
| Type de bump | MINOR |
| Justification | Prise en charge du standard d'échange de résultats d'analyses FHIR |
