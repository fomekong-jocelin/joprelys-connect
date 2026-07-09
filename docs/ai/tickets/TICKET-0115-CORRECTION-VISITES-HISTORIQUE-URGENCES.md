# TICKET-0115 — Correction crash visites et historique des soins d'urgences

## 1. Description et contexte

**Epic** : EPIC-0014 — Alignement des modules 4 à 12 du CDC
**Titre** : Correction crash visites et historique des soins d'urgences
**Statut** : **DONE**
**Priorité** : P0
**Sprint** : SPRINT-0011
**Profil recommandé** : Senior Full-stack
**Estimation** : 0.5j

## 2. Objectifs et Problématiques résolues

1. **Bug 500 dans la vue Hospitalisations/Visites du Patient** :
   - *Problème* : L'accès à la route `/patients/:id/hospitalizations` provoquait une erreur 500 sur le endpoint backend `/api/visits/patient/{patientId}`.
   - *Cause* : Une `LazyInitializationException` survenait en dehors de la transaction `@Transactional` lors du mapping du modèle `VisitEntity` vers le DTO `VisitResponse` (chargement paresseux de l'objet `patient` et des constantes `vitals`).
   - *Correction* : Ajout de la méthode optimisée `findByPatientIdWithPatientAndVitals` dans `VisitRepository` utilisant un `JOIN FETCH` pour charger le patient et les constantes en un seul appel SQL. Mise à jour de `VisitService` pour utiliser cette méthode.

2. **Perte de l'historique de réanimation lors de la stabilisation d'une urgence** :
   - *Problème* : Lorsqu'une urgence était stabilisée (`stabilizedAt != null`), le patient n'était plus listé sur le tableau de bord des urgences, faisant perdre toute visibilité sur les soins et bolus de réanimation administrés.
   - *Correction* :
     - Ajout de la requête `findByPatientIdWithLogs` dans `EmergencyRepository`.
     - Exposition de l'endpoint backend `GET /api/emergencies/patient/{patientId}`.
     - Intégration de la section **🚨 Passages aux Urgences** dans le composant de synthèse médicale du patient (`PatientMedicalInfoComponent`). Cet historique récapitule chronologiquement toutes les admissions aux urgences du patient, leurs niveaux de triage, constantes d'admission, orientations post-stabilisation, et le détail complet de chaque geste de réanimation.

## 3. Critères d'acceptation (DoD)

- [x] Pas d'erreur 500 sur l'API `/api/visits/patient/{patientId}` (tests unitaires et intégration valides).
- [x] L'historique des urgences et de réanimation d'un patient est accessible via l'API et est affiché de manière élégante et i18n-ready dans son Dossier Médical.
- [x] Tous les tests backend passent avec succès.
- [x] La compilation Angular de production réussit sans erreur.

## 4. Fichiers impactés

### Backend
- [VisitRepository.java](file:///C:/MES-APPLICATIONS/joprelys-connect/backend/src/main/java/com/joprelys/backend/visit/infrastructure/persistence/VisitRepository.java)
- [VisitService.java](file:///C:/MES-APPLICATIONS/joprelys-connect/backend/src/main/java/com/joprelys/backend/visit/application/VisitService.java)
- [EmergencyRepository.java](file:///C:/MES-APPLICATIONS/joprelys-connect/backend/src/main/java/com/joprelys/backend/emergency/infrastructure/persistence/EmergencyRepository.java)
- [EmergencyService.java](file:///C:/MES-APPLICATIONS/joprelys-connect/backend/src/main/java/com/joprelys/backend/emergency/application/EmergencyService.java)
- [EmergencyController.java](file:///C:/MES-APPLICATIONS/joprelys-connect/backend/src/main/java/com/joprelys/backend/emergency/api/EmergencyController.java)
- [EmergencyControllerTest.java](file:///C:/MES-APPLICATIONS/joprelys-connect/backend/src/test/java/com/joprelys/backend/emergency/api/EmergencyControllerTest.java)

### Frontend
- [emergency-api.service.ts](file:///C:/MES-APPLICATIONS/joprelys-connect/web/src/app/emergency/emergency-api.service.ts)
- [patient-medical-info.component.ts](file:///C:/MES-APPLICATIONS/joprelys-connect/web/src/app/patient/patient-medical-info.component.ts)
