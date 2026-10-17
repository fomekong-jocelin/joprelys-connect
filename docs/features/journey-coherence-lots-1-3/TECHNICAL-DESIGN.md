# TECHNICAL-DESIGN — Cohérence des parcours, lots 1 à 3

## Migrations Flyway

| Version | Contenu |
|---|---|
| V116 | `prescriptions.dispensation_status` (NONE/PARTIAL/FULL) ; reprise de `*_DISPENSED` en `ACTIVE`. |
| V117 | `medication_administrations.client_request_id` (unique par établissement), `override_reason`. |
| V118 | `emergencies.orientation_decided_at/by`, `stabilized_by` ; `patients.deceased_at`, `death_declared_by/at`, `death_place`, `death_circumstances`. |
| V119 | `visits.cancellation_reason`, `cancellation_note`, `cancelled_by`. |
| V120 | `invoice_items.source_type/source_id/tariff_key/tariff_missing` ; `prescription_dispensations.dispensing_organization_id` ; `organizations.lab_payment_before_sampling`, `credit_note_approval_threshold` ; `credit_notes.issued_by/approved_by/approved_at/refunded_amount` ; table `patient_deposits` ; `payments.deposit_id`. |

Toutes les migrations ont été validées sur PostgreSQL 17 (dry run dans une transaction annulée) et sur H2.

## API

| Méthode | Chemin | Permission | Note |
|---|---|---|---|
| POST | `/api/visits/{id}/close` | inchangée | 409 si un autre praticien a le patient en charge ou si un séjour est en cours |
| POST | `/api/visits/{id}/abandon` | `VISIT_CREATE` | `{reason, note}` |
| POST | `/api/visits/{id}/cancel` | `VISIT_MANAGE` | corps facultatif `{reason, note}` |
| POST | `/api/hospitalizations/{id}/medication-administrations` | inchangée | `clientRequestId`, `overrideReason` facultatifs |
| PATCH | `/api/lab-orders/{id}/status` | inchangée | `reason` obligatoire pour `CANCELLED` ; progression refusée (409) pour une demande détaillée |
| POST | `/api/emergencies/{id}/stabilize` | inchangée | orientation contrôlée ; réponse `orientationDecidedAt`, `awaitingDownstream`, `stabilizedBy` |
| POST | `/api/patients/{id}/death`, `/death/revoke` | `PATIENT_DEATH_DECLARE` | |
| POST | `/api/hospitalizations/{id}/discharge/cancel` | `HOSPITALIZATION_DISCHARGE_DECIDE` | `{reason}` |
| POST | `/api/invoices/{id}/recalculate` | `BILLING_INVOICE_WRITE` | facture `PENDING` |
| POST | `/api/invoices/{id}/apply-deposit` | `CASH_PAYMENT_COLLECT` | `{depositId, amount?}` |
| POST/GET | `/api/deposits` | `CASH_PAYMENT_COLLECT` / lecture | |
| POST | `/api/invoices/{id}/credit-notes` | `BILLING_CREDIT_NOTE_ISSUE` ou `BILLING_INVOICE_CANCEL` | statut `PENDING_APPROVAL` au-delà du seuil |
| POST | `/api/credit-notes/{id}/approve`, `/reject` | `BILLING_CREDIT_NOTE_APPROVE` | jamais par l'émetteur |
| POST | `/api/credit-notes/{id}/refund` | `CASH_PAYMENT_COLLECT` | session ouverte |
| GET/PUT | `/api/billing/settings` | lecture / `BILLING_SETTINGS_MANAGE` | |

**Contrats modifiés :**
- Une ordonnance délivrée reste `status = ACTIVE` ; la délivrance passe dans `dispensationStatus` : **MAJOR candidat**.
- Plusieurs factures sont désormais permises par visite ; la création renvoie 409 « Aucune charge restant à facturer » s'il ne reste rien.
- Le prix des médicaments vient de la délivrance.

## Composants principaux

- **Backend :**
  - Nouveaux services et politiques : `VisitAbandonmentService`, `PatientDeathService`, `MedicationAdministrationWindowPolicy`, `LabPaymentGatePolicy`, `OrganizationalUnitUsageQuery`.
  - Facturation : `InvoiceChargeCollector`, `BilledSources`, `CreditNoteService`, `PatientDepositService`, `LabPaymentReleaseService`, `BillingSettingsService`.
  - `ConventionTariffService.findTariff` remplace `getTariff(clé, défaut)`.
- **Angular :**
  - Nouveaux composants : `patient-death-panel`, `billing-credit-notes-panel`, `billing-tariff-alert`, `billing-deposits-panel`, `billing-settings-panel`.
  - Écrans adaptés : file d'accueil, tableau des urgences, panneau d'administration médicamenteuse, en-tête de séjour, pharmacie, laboratoire.

## Rôles système

- `MEDECIN` : + `PATIENT_DEATH_DECLARE`.
- `DAF` : + `BILLING_INVOICE_CANCEL`, `BILLING_CREDIT_NOTE_ISSUE`, `BILLING_CREDIT_NOTE_APPROVE`, `BILLING_SETTINGS_MANAGE`.
- `SECRETAIRE_COMPTABLE` : + `BILLING_INVOICE_CANCEL`, `BILLING_CREDIT_NOTE_ISSUE`.
- `CAISSIER` : + `PATIENT_READ` (D-18).
- `ADMIN_CLINIQUE` : reçoit les nouvelles permissions de facturation, mais pas `PATIENT_DEATH_DECLARE`.

Les rôles personnalisés n'héritent de rien : à attribuer manuellement.

## Tests

- **Backend : 959 tests, 0 échec**, exécutés par lots (`-Xmx900m`, mémoire disponible limitée). 9 tests de migration PostgreSQL sont ignorés faute de Docker, comme avant.
  - Nouveaux tests : `JourneyContinuityControllerTest`, `BillingJourneyControllerTest`, `MedicationAdministrationWindowPolicyTest`, `InvoicePrecalculationServiceTest` (réécrit) ; ajouts dans `VisitControllerTest`, `EmergencyControllerTest`, `HospitalizationWorkflowControllerTest` et `LabOrderControllerTest`.
- **Angular :** 120 fichiers, 663 tests verts ; build de développement OK ; `i18n:check` OK.

## Limites connues

- Pas de recette navigateur.
- Le plan d'administration complet, la corbeille de résultats et les notifications du personnel relèvent des lots 4-5.
- La facturation du séjour se fait sur l'espace courant (FA-20).
- Les délivrances historiques n'ont pas d'établissement délivreur et ne sont donc pas facturées.
- `lab-orders-page.component.ts` (763 lignes) et `patient-hospitalization.component.ts` (466 lignes) restent une dette de taille existante.
