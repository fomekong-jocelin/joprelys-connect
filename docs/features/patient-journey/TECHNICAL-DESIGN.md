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

## Parcours de prise en charge (2026-10-03, second lot)

### Backend
- **Flyway V112** :
  - `visits` reçoit les colonnes `care_stage` (défaut `ATTENTE_CONSTANTES`), `consulting_practitioner_id`, `consulting_practitioner_name` et `consultation_started_at`, avec reprise de l'existant : constantes présentes → `PRET_MEDECIN`, consultation présente → `EN_CONSULTATION` ;
  - nouvelle table `visit_vital_measurements`, alimentée par reprise de la table `vitals`.
- `VisitEntity` porte les transitions : `markVitalsRecorded`, `startConsultation`, `releaseConsultation`, `isInConsultationWithAnotherPractitioner`.
- `VisitCareFlowUseCase` / `VisitCareFlowService` :
  - `POST /api/visits/{id}/take-charge?takeOver=` et `POST /api/visits/{id}/release`, sous la permission `CLINICAL_WRITE` ;
  - `ensureConsultationOwnership`, appelé par `ConsultationService.saveConsultation`.
- `VisitVitalsUseCase` / `VisitVitalsService` : ce service reprend la saisie des constantes, auparavant dans `VisitService`, et y ajoute l'historique et le passage à l'étape « prêt pour le médecin ». Endpoints exposés par `VisitVitalsController` :
  - `POST` et `GET /api/visits/{id}/vitals` ;
  - `GET /api/visits/{id}/vitals/history`.
- `VitalSignAlertPolicy` (domaine, fonction pure) : alertes exposées dans `VitalsResponse.alerts` et `VitalMeasurementResponse.alerts`.
- `ActiveVisitQueueUseCase` : `GET /api/visits/active?scope=ALL|MINE|SERVICE`. Le scope `SERVICE` compare `service_name` aux unités actives du praticien (`StaffProfileAssignmentService`).
- `VisitResponse` expose en plus `careStage`, `consultingPractitionerId`, `consultingPractitionerName` et `consultationStartedAt`.

### Front
- Le tableau de bord est découpé en quatre composants :

| Composant | Rôle | Taille |
|---|---|---|
| `DashboardComponent` | Accueil, rôle, modale d'audit | 69 lignes |
| `ActiveVisitQueueComponent` | File, filtres, résumé, clôture | — |
| `VisitDetailsDrawerComponent` | Tiroir : étape, alertes, historique, actions | — |
| `VisitVitalsFormModalComponent` | Saisie d'une mesure | — |

- Composants partagés dans `visit/` :
  - `VitalAlertsComponent` : pastilles d'alerte ;
  - `VitalsHistoryComponent` : historique, aussi affiché dans la consultation ;
  - `vitals-display.util` : couleurs d'IMC et d'étape ;
  - `visit-orientation.util` : codes d'orientation et libellés, avec repli sur le texte libre.
- Admission :
  - `VisitDetailsFieldsComponent`, `visit-details-form.ts` (contrôles et mapping vers `CreateVisitRequest`) et `VisitAdmissionOptionsService` (catalogue des services et cliniciens), partagés par `UnifiedAdmissionComponent` (504 → 432 lignes) et `PatientVisitAdmissionDialogComponent` ;
  - `PatientSearchPickerComponent` : recherche avec délai de 300 ms, 2 caractères minimum, 10 résultats au plus.

### Tests
- Backend :
  - `VisitCareFlowControllerTest` : étapes, historique et alertes, reprise de patient, remise dans la file, prise en charge implicite, scope `MINE` ;
  - `VitalSignAlertPolicyTest`.
- Front :
  - `active-visit-queue`, `visit-details-drawer` et `visit-vitals-form-modal` (migrés depuis la spec du tableau de bord) ;
  - `patient-search-picker` ;
  - `unified-admission` : formulaire commun, pas d'orientation « Urgences ».
