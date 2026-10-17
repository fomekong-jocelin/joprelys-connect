# TECHNICAL-DESIGN — Audit fonctionnel de bout en bout (2026-10-17)

## Méthode technique

1. **Cartographie statique** : contrôleurs REST (324 mappings), routes Angular (55 entrées), `RbacCatalog` (17 rôles système, 85 permissions), enums et champs de statut des entités, services applicatifs des parcours.
2. **Environnement isolé** : le backend est relancé depuis les classes compilées de l'instance de développement, sur le port 8095, avec la base PostgreSQL dédiée `joprelys_audit_e2e_20261017`. Les migrations Flyway sont appliquées sur une base vide, l'IA est désactivée et MailDev capture les e-mails.
3. **Scénarios API multi-rôles** ([scenarios/](scenarios/)) : chaque intervenant se connecte avec son propre compte (mot de passe temporaire + OTP) ; les transitions sont jouées dans l'ordre métier ; la concurrence est testée par requêtes parallèles.

## Points de code cités par les constats

| Constat | Emplacement |
|---|---|
| FA-01 | `visit/application/VisitService.closeVisit`, `VisitClinicalClosureService.signClinicalActs`, `VisitCareFlowService.ensureConsultationOwnership` |
| FA-02 | `hospitalization/application/HospitalizationMedicationPolicy.eligibleItems`, `prescription/application/PharmacyService` (statuts `*_DISPENSED`) |
| FA-04 / FA-05 | `HospitalizationDischargeWorkflowService.confirmPhysicalDeparture`, `HospitalizationService.dischargePatient` |
| FA-06 / FA-08 | `emergency/application/EmergencyService.stabilizeEmergency`, `EmergencyController.stabilize`, `web/src/app/emergency/emergency-dashboard.component.ts` |
| FA-09 / FA-10 | `lab/application/LabOrderService.updateStatus`, `create`, `mergeMissingExams` |
| FA-11 | `lab/application/LabResultService.notifyPatient`, `getPatientResults` ; `notification/infrastructure/persistence/NotificationEntity` |
| FA-13 / FA-14 / FA-15 / FA-16 | `billing/application/InvoicePrecalculationService`, `ConventionTariffService.getTariff`, `InvoiceItemType` |
| FA-23 | `appointment/infrastructure/persistence/AppointmentEntity.linkVisit` (aucun appelant) |
| FA-24 | `visit/application/VisitService.createVisit`, `VisitEntity` (`service_name` texte) |
| FA-28 | `VisitService.propagateEmergencyVitalsIfApplicable` |
| FA-29 | `hospitalorganization/application/DefaultHospitalOrganizationService.setUnitActive` |
| FA-30 | `spatial/api/TransferRequest`, `HospitalizationEntity.setResponsiblePractitionerId` (aucun appelant) |
| FA-37 | `auth/rbac/RbacCatalog.systemRoles` (`ADMIN_CLINIQUE = without(all, …)`) |

## Impact

Aucun code applicatif, contrat API, migration ni donnée réelle modifiés. Les seuls ajouts sont documentaires (`docs/`) et les scripts de scénarios.
