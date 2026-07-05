# STORY-1701 — Mapping des entités DPU vers les ressources FHIR (Patient, Encounter, Observation)

## 1. Description et contexte
**Epic** : Interopérabilité HL7 FHIR (EPIC-0011)  
**Titre** : Mapping des entités DPU vers les ressources FHIR (Patient, Encounter, Observation)  
**Statut** : DONE  
**Priorité** : P1  
**Sprint** : SPRINT-0009  
**SP** : 5  
**Profil recommandé** : Senior  
**Estimation** : 1.5j (Senior: 1.0j, Junior: 2.5j) | **Temps passé** : 0.2j  

## 2. Objectifs
Implémenter la couche de conversion (Mappers) traduisant les entités JPA existantes en DTOs structurés conformes à la spécification HL7 FHIR R4.

## 3. Critères d'acceptation (DoD)
- [x] Création du package `com.joprelys.backend.fhir` contenant les DTOs FHIR minimaux (`FhirPatientDto`, `FhirEncounterDto`, `FhirObservationDto`).
- [x] Création des classes utilitaires ou de mapping :
  - `FhirPatientMapper.toFhir(PatientEntity)`
  - `FhirEncounterMapper.toFhir(VisitEntity)`
  - `FhirObservationMapper.toFhir(VitalsEntity)` (LOINC codes pour Température, Poids, Tension, Pouls).
- [x] Tests unitaires couvrant les différents cas de mapping (noms vides, genres inconnus, conversion de types de dates).
- [x] Zéro dépendance externe lourde (Jackson et structures Java pures préférés).

## 4. Reste à faire
Aucun. STORY-1701 est terminée.
