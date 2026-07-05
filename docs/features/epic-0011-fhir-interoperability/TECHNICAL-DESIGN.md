# Design Technique — Interopérabilité HL7 FHIR (EPIC-0011)

## 1. Stack et technologies
- **Backend** : Spring Boot, Maven, Jackson pour la sérialisation JSON.
- **Standards** : HL7 FHIR R4 JSON Schemas.

## 2. Modèle de données & Architecture
Les données restent stockées dans les tables existantes (`patients`, `visits`, `vital_signs`).
Un package applicatif dédié `com.joprelys.backend.fhir` sera créé avec :
- Des convertisseurs (Mappers) : `FhirPatientMapper`, `FhirEncounterMapper`, `FhirObservationMapper` traduisant nos entités JPA en DTOs structurés FHIR.
- Des DTOs Java mimant la structure FHIR R4 minimalement requise (pour éviter d'embarquer la bibliothèque lourde HAPI FHIR et rester léger).

### Mapping de données
1. **Patient -> FHIR Patient** :
   - `id` -> `id`
   - `globalPatientNumber` -> `identifier` (system: `urn:oid:1.3.6.1.4.1.59367.1.1`)
   - `fullName` -> `name` (split text in family/given)
   - `gender` -> `gender` (MASCULIN -> male, FEMININ -> female)
   - `birthDate` -> `birthDate`
   - `phone` -> `telecom` (system: phone)

2. **Visit -> FHIR Encounter** :
   - `id` -> `id`
   - `visitNumber` -> `identifier`
   - `status` (EN_COURS/TERMINEE) -> `status` (in-progress/finished)
   - `arrivalAt` -> `period.start`
   - `closedAt` -> `period.end`
   - `patientId` -> `subject.reference` ("Patient/{id}")

3. **VitalSigns -> FHIR Observation** :
   - Chaque constante (température, poids, tension) génère une ressource `Observation` distincte liée à la visite (Encounter) et au patient (subject).
   - Utilisation de codes LOINC standardisés pour les constantes (ex: `8310-5` pour la température corporelle, `29463-7` pour le poids corporel).

## 3. Contrats d'API REST
Les endpoints suivants seront exposés sous le préfixe `/fhir` :
* `GET /fhir/Patient/{id}` : Retourne une ressource FHIR `Patient`.
* `GET /fhir/Encounter/{id}` : Retourne une ressource FHIR `Encounter`.
* `GET /fhir/Observation?patient={patientId}` : Retourne un `Bundle` de ressources `Observation` pour un patient donné.

## 4. Sécurité & Contrôles
- **Permissions** : Endpoints réservés aux utilisateurs avec rôles `MEDECIN`, `INFIRMIER`, `BIOLOGISTE`, ou clients d'API machine-to-machine avec scope `fhir.read`.
- **Tenant Isolation** : Application des filtres Hibernate `@TenantId` pour garantir que les clients d'API ou praticiens ne lisent que les données de leur organisation.

## 5. Stratégie de tests
- **Tests Unitaires** : Validation du mapping Java des convertisseurs (ex. `FhirPatientMapperTest` pour s'assurer que les dates et noms sont bien formatés en FHIR).
- **Tests d'Intégration** : Validation MockMvc de `GET /fhir/Patient/{id}` et vérification de la structure JSON renvoyée (comparaison contre des schémas d'exemples FHIR R4).
- **Tests de Sécurité** : Vérification de l'interdiction de lecture cross-tenant et des accès non autorisés.
