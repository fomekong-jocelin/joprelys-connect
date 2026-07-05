# STORY-1702 — Endpoints REST FHIR pour les patients et consultations

## 1. Description et contexte
**Epic** : Interopérabilité HL7 FHIR (EPIC-0011)  
**Titre** : Endpoints REST FHIR pour les patients et consultations  
**Statut** : TODO  
**Priorité** : P1  
**Sprint** : SPRINT-0009  
**SP** : 5  
**Profil recommandé** : Senior  
**Estimation** : 1.5j (Senior: 1.0j, Junior: 2.5j)  

## 2. Objectifs
Exposer des endpoints REST sécurisés retournant les structures FHIR R4 générées par le mapper.

## 3. Critères d'acceptation (DoD)
- [ ] Contrôleur `FhirController` exposant :
  - `GET /fhir/Patient/{id}`
  - `GET /fhir/Encounter/{id}`
  - `GET /fhir/Observation?patient={patientId}` (renvoyant un Bundle FHIR R4).
- [ ] Sécurisation par Spring Security (limité aux rôles `MEDECIN`, `INFIRMIER`, `BIOLOGISTE`, ou scopes d'API de confiance).
- [ ] Application stricte de l'isolation multi-tenant de clinique (un praticien de la Clinique A ne peut lire un patient de la Clinique B qu'avec son consentement).
- [ ] Journalisation automatique de l'accès dans les logs d'audit avec l'action `READ_FHIR_RESOURCE`.
- [ ] Tests d'intégration MockMvc complets validant le format JSON et les contraintes de sécurité.

## 4. Reste à faire
- [ ] Créer le contrôleur `FhirController`.
- [ ] Configurer les filtres de sécurité Spring Security.
- [ ] Rédiger les tests d'intégration MockMvc.
