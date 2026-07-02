# TICKET-0502-0504 — Module Prescription + Historique Consultations (Backend)

## Contexte

| Champ | Valeur |
|---|---|
| Stories | STORY-0502 (Prescription médicale), STORY-0504 (Historique consultations) |
| Mode | Engineering |
| Sprint | Sprint courant |
| Statut | DONE ✅ |
| Profil | Backend Spring Boot Expert |

## Périmètre

- Création du module `prescription` (entités, repository, service, contrôleur)
- Ajout de la route historique `/api/patients/{patientId}/consultations`
- Migration Flyway V7 : tables `prescriptions` et `prescription_items`
- Tests d'intégration MockMvc (`PrescriptionControllerTest`)

## Actions

- [x] Créer `V7__create_prescription_table.sql`
- [x] Créer `PrescriptionItemEntity.java`
- [x] Créer `PrescriptionEntity.java` (avec `@TenantId` pour multi-tenant)
- [x] Créer `PrescriptionRepository.java`
- [x] Créer DTOs : `PrescriptionItemRequest`, `SavePrescriptionRequest`, `PrescriptionItemResponse`, `PrescriptionResponse`
- [x] Créer `PrescriptionService.java` (upsert pattern + orphanRemoval)
- [x] Créer `PrescriptionController.java` (`/api/consultations/{id}/prescription`)
- [x] Modifier `ConsultationRepository.java` : ajouter `findByPatientIdOrderByCreatedAtDesc`
- [x] Modifier `ConsultationService.java` : ajouter `getConsultationsByPatientId`
- [x] Créer `ConsultationHistoryController.java` (`/api/patients/{patientId}/consultations`)
- [x] Créer `PrescriptionControllerTest.java` (9 cas de test)
- [ ] Validation BUILD SUCCESS des tests

## Fichiers créés/modifiés

### Nouveaux fichiers
- `backend/src/main/resources/db/migration/V7__create_prescription_table.sql`
- `backend/.../prescription/infrastructure/persistence/PrescriptionItemEntity.java`
- `backend/.../prescription/infrastructure/persistence/PrescriptionEntity.java`
- `backend/.../prescription/infrastructure/persistence/PrescriptionRepository.java`
- `backend/.../prescription/api/PrescriptionItemRequest.java`
- `backend/.../prescription/api/SavePrescriptionRequest.java`
- `backend/.../prescription/api/PrescriptionItemResponse.java`
- `backend/.../prescription/api/PrescriptionResponse.java`
- `backend/.../prescription/application/PrescriptionService.java`
- `backend/.../prescription/api/PrescriptionController.java`
- `backend/.../consultation/api/ConsultationHistoryController.java`
- `backend/src/test/.../prescription/api/PrescriptionControllerTest.java`

### Fichiers modifiés
- `backend/.../consultation/infrastructure/persistence/ConsultationRepository.java` (+méthode +import)
- `backend/.../consultation/application/ConsultationService.java` (+méthode +import)

## Tests prévus (PrescriptionControllerTest)

| # | Test | Expect |
|---|---|---|
| 1 | givenMedecinA_whenSavePrescription_thenSuccess | 200, items.length==2 |
| 2 | givenExistingPrescription_whenSaveAgain_thenUpsertSuccess | 200, items.length==1 |
| 3 | givenMedecinA_whenGetPrescription_thenSuccess | 200, items.length>=1 |
| 4 | givenAgentAccueil_whenSavePrescription_thenForbidden | 403 |
| 5 | givenEmptyItems_whenSavePrescription_thenBadRequest | 400 |
| 6 | givenUnknownConsultation_whenSavePrescription_thenNotFound | 404 |
| 7 | givenNoPrescription_whenGetPrescription_thenNotFound | 404 |
| 8 | givenNoToken_whenSavePrescription_thenUnauthorized | 401 |
| 9 | givenMedecinB_whenSavePrescriptionOnConsultationA_thenNotFound | 404 |

## Sécurité / Régression

- Isolation multi-tenant via `@TenantId` Hibernate (même pattern que `ConsultationEntity`)
- Contrôle d'accès par `@PreAuthorize` sur chaque endpoint
- Pas de modification des API existantes
- `orphanRemoval = true` garantit la suppression des anciens items à chaque upsert

## Impact version / SemVer

- MINOR : nouveaux endpoints, nouvelle table DB, rétrocompatible

## Reste à faire

- Valider BUILD SUCCESS des tests
- Mettre à jour `PROJECT-TRACKING.md`
- Mettre à jour `CHANGELOG.md`
