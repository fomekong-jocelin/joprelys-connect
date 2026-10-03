# Parcours patient — conception technique des correctifs du 2026-10-03

| Sujet | Backend | Front |
|---|---|---|
| Bon d'examens unique | `LabOrderService.create` : recherche `findFirstByVisitIdAndExamTypeAndStatusNotOrderByCreatedAtDesc(..., CANCELLED)` ; s'il existe, `mergeMissingExams` (ajout insensible à la casse, recalcul du statut seulement en cas d'ajout) | inchangé |
| Consultation concurrente | `SaveConsultationRequest.expectedUpdatedAt` (optionnel) ; comparaison à la milliseconde avec `updatedAt` → `409 CONFLICT` ; suppression du `setDoctor` sur mise à jour | `ConsultationComponent` envoie `consultation().updatedAt` |
| Constantes | `VisitService.saveVitals(visitId, request, actorUserId, actorOrgId)` : contrôle systolique > diastolique, audit `VISIT_VITALS_RECORDED` (JSON des valeurs) ; `VitalsResponse.recordedAt` | `DashboardComponent.isBloodPressureInconsistent` (bloquant), `isGlycemiaUnitSuspicious` (avertissement) ; affichage de `recordedAt` en consultation |
| Pré-enregistrement | `GET /api/pre-registrations?status=` (défaut `AWAITING_VALIDATION`) ; colonne `validated_patient_id` (Flyway **V111**) ; `GET /api/pre-registrations/admission-qr-code` (PNG, `PATIENT_WRITE`) via `GenerateAdmissionQrCodeUseCase` | `PatientApiService.getPreRegistrations(status, page, size)`, `getAdmissionQrCode()` (blob → object URL, libéré au destroy) ; i18n FR/EN des libellés codés en dur |
| Admission | — | brouillon en `sessionStorage` ; `orientationOptions` sans `EMERGENCY` ; praticiens = `MEDECIN` / `INFIRMIER` |
| Consultation (architecture) | — | suppression de `HttpClient` dans le composant (`VisitApiService.getById`, `getVitals`) |

## Compatibilité
- `expectedUpdatedAt` est optionnel : les clients qui ne l'envoient pas (mobile) gardent le comportement actuel, sans vérification de concurrence.
- `getPendingPreRegistrations` est conservé comme alias (badge de navigation).
- V111 ajoute une colonne nullable : les données existantes ne sont pas touchées. Les demandes validées avant V111 n'ont pas de `validatedPatientId` ; le front se rabat alors sur `similarPatientId`, sinon le bouton est masqué.

## Tests
- Backend :
  - `LabOrderItemWorkflowTest.resavingConsultationOnlyAddsMissingExamsToTheExistingRequest` ;
  - `VisitControllerTest` : tension incohérente → 400, `recordedAt` présent ;
  - `PatientPreRegistrationControllerTest` : filtre `VALIDATED` + `validatedPatientId`, QR PNG.
- Front : `pre-registrations-list.component.spec.ts` (filtre serveur, patient lié, QR serveur).
