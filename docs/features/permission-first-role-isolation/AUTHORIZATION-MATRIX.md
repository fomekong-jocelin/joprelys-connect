# AUTHORIZATION-MATRIX — Autorisation permission-first

## Principe

Chaque endpoint sensible exige exclusivement une ou plusieurs authorities métier. Un rôle
système n'est qu'un ensemble de permissions initiales ; un rôle personnalisé est autorisé
de la même manière lorsqu'il reçoit la même permission. Aucun `hasRole`, `hasAnyRole` ou
fallback `permission OR rôle` n'est admis dans les contrôleurs.

Les contrôles de tenant, de propriété patient, de consentement et d'état métier restent
obligatoires après le contrôle fonctionnel.

## Matrice canonique

| Domaine / action | Permission exigée |
|---|---|
| Utilisateurs — lecture | `USER_READ` ou `USER_MANAGE` |
| Utilisateurs — gestion | `USER_MANAGE` |
| RBAC — lecture | `RBAC_READ` ou `RBAC_MANAGE` |
| RBAC — gestion | `RBAC_MANAGE` |
| Organisations | `ORGANIZATION_MANAGE` |
| Webhooks | `WEBHOOK_MANAGE` |
| Profil collaborateur | `STAFF_PROFILE_ACCESS` |
| Téléversement de fichiers | `FILE_UPLOAD` |
| Disponibilités — lecture | `AVAILABILITY_READ` |
| Disponibilités — gestion de ses propres créneaux | `AVAILABILITY_MANAGE` |
| Disponibilités — gestion de tous les praticiens autorisés | `AVAILABILITY_MANAGE_ALL` |
| Patients — lecture | `PATIENT_READ` |
| Patients — écriture / préinscription | `PATIENT_WRITE` |
| Patients — fusion | `PATIENT_MERGE` |
| Informations médicales — lecture | `CLINICAL_READ` |
| Informations médicales — écriture | `CLINICAL_WRITE` |
| Visites — lecture / QR | `VISIT_READ` |
| Visites — création | `VISIT_CREATE` |
| Visites — constantes | `VISIT_VITALS_WRITE` |
| Visites — correction / clôture / annulation | `VISIT_MANAGE` |
| Documents médicaux — lecture | `DOCUMENT_READ` |
| Documents médicaux — révocation / annulation | `DOCUMENT_MANAGE` |
| Consultations — lecture | `CLINICAL_READ` |
| Consultations — écriture / assistant IA | `CLINICAL_WRITE` |
| Prescriptions — lecture clinique | `CLINICAL_READ` |
| Prescriptions — lecture pharmacie | `PHARMACY_PRESCRIPTION_READ` |
| Prescriptions — écriture | `CLINICAL_WRITE` |
| Urgences | permissions `EMERGENCY_*` existantes |
| Laboratoire | permissions `LAB_*` existantes |
| FHIR Patient | `PATIENT_READ` |
| FHIR Encounter / Observation | `CLINICAL_READ` |
| FHIR DiagnosticReport | `LAB_ORDER_READ` |
| Hospitalisation / spatial | permissions `HOSPITALIZATION_*` existantes |
| Configuration spatiale | `SPATIAL_CONFIGURATION_MANAGE` |
| Accueil — lecture | `RECEPTION_READ` |
| Accueil — écriture | `RECEPTION_WRITE` |
| Accès externe clinique | `PATIENT_EMERGENCY_ACCESS` |
| Facturation | permissions `BILLING_*` existantes |
| Relances créances — lecture | `RECEIVABLE_REMINDER_READ` |
| Relances créances — écriture | `RECEIVABLE_REMINDER_WRITE` |
| Caisse | permissions `CASH_*` existantes |
| Assurance | permissions `INSURANCE_*` existantes |
| Comptabilité | permissions `ACCOUNTING_*` existantes |
| Audit tenant | `AUDIT_READ` |
| Audit inter-établissements | `AUDIT_CROSS_TENANT_READ` |
| Stock pharmacie | `PHARMACY_STOCK_MANAGE` |
| Portail patient | `PATIENT_PORTAL_ACCESS` |
| Rendez-vous patient | `PATIENT_APPOINTMENT_MANAGE` |
| Notifications patient | `PATIENT_NOTIFICATION_MANAGE` |

## Affectations système ajoutées

| Rôle | Permissions ajoutées |
|---|---|
| `AGENT_ACCUEIL` | `VISIT_READ`, `VISIT_CREATE`, `VISIT_VITALS_WRITE`, `DOCUMENT_READ`, `RECEPTION_READ`, `RECEPTION_WRITE` |
| `INFIRMIER` | `VISIT_READ`, `VISIT_CREATE`, `VISIT_VITALS_WRITE`, `DOCUMENT_READ`, `RECEPTION_READ` |
| `MEDECIN` | `USER_READ`, `VISIT_READ`, `VISIT_CREATE`, `VISIT_VITALS_WRITE`, `VISIT_MANAGE`, `DOCUMENT_READ`, `DOCUMENT_MANAGE`, `RECEPTION_READ` |
| `PHARMACIEN` | `DOCUMENT_READ` |
| `DAF` | `RECEIVABLE_REMINDER_READ`, `RECEIVABLE_REMINDER_WRITE` |
| `SECRETAIRE_COMPTABLE` | `RECEIVABLE_REMINDER_READ`, `RECEIVABLE_REMINDER_WRITE` |
| `AUDITEUR` | `AUDIT_CROSS_TENANT_READ` |
| `ADMIN_JOPRELYS` | `ORGANIZATION_MANAGE`, `WEBHOOK_MANAGE`, `SPATIAL_CONFIGURATION_MANAGE`, `USER_READ`, `RBAC_READ`, `RBAC_MANAGE` |
| `SUPER_ADMIN` | `ORGANIZATION_MANAGE`, `SPATIAL_CONFIGURATION_MANAGE`, `USER_READ`, `RBAC_READ`, `RBAC_MANAGE` |
| `ADMIN_CLINIQUE` | toutes les permissions tenant sauf `ORGANIZATION_MANAGE`, `AUDIT_CROSS_TENANT_READ` et les trois capacités patient |
| `PATIENT` | capacités patient dédiées, injectées uniquement pour un jeton patient |

## Refus intentionnels issus de la suppression des fallbacks

- un médecin sans `AUDIT_READ` ne lit plus les journaux d'audit ;
- un infirmier sans permission de facturation ne lit plus les factures ou devis ;
- un rôle clinique sans `USER_READ` ne liste plus les collaborateurs ; le médecin conserve cette lecture pour les sélecteurs de praticiens ;
- un administrateur plateforme ne lit plus automatiquement les données cliniques d'un tenant ;
- `AVAILABILITY_MANAGE` n'autorise jamais la gestion des créneaux d'un autre praticien ; ce périmètre exige `AVAILABILITY_MANAGE_ALL` ;
- retirer une permission d'un rôle personnalisé retire immédiatement menu, route et API.
