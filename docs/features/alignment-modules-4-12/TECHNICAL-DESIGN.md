# TECHNICAL-DESIGN — Alignement modules 4 à 12 du CDC

> Feature ID : `alignment-modules-4-12`  
> Epic : `EPIC-0014`  
> Ticket principal : `TICKET-0114`  
> Date : 2026-07-05  
> Version : 1.0

---

## 1. Architecture cible

### Backend

```text
Controller → Application Service / Use Case → Domain ← Infrastructure (JPA)
```

- Aucun controller n’appelle directement un repository.
- `PatientService` est découpé en services spécialisés.
- Les entités JPA ne sont jamais exposées en réponse API.
- Les statuts métier sont modélisés par des enums avec contrainte CHECK DB.

### Frontend

```text
Component UI → Facade / Service → HttpClient → API
```

- Composants centrés sur la présentation.
- Logique métier dans les services/facades.
- Design system Tailwind CSS v4 CSS-first.
- i18n externalisée dans `assets/i18n/`.

---

## 2. Modèle de données

### Tables modifiées / créées

| Migration | Tables / colonnes |
|---|---|
| V30 | `patient_medical_history.important BOOLEAN` |
| V31 | `visits.service`, `visits.main_practitioner_id`, `visits.arrival_at` ; `consultations.suspected_diagnosis`, `consultations.final_diagnosis`, `consultations.conclusion` ; `vital_signs.pain_scale` |
| V32 | `patient_medical_history.deleted_at`, `deleted_by` ; `patient_allergies.deleted_at`, `deleted_by` |
| V33 | `prescriptions.issued_at`, `prescriptions.visit_id`, `prescriptions.document_id` ; `prescription_items.form`, `route`, `frequency`, `substitution_allowed` |
| V34 | `lab_order_items` (table fille) ; `lab_orders.source_organization_id` ; enum statuts |
| V35 | `lab_results.status`, `validator_user_id`, `conclusion`, `document_id`, `version`, `parent_result_id` |
| V36 | `hospitalizations.hospitalization_number`, `visit_id`, `responsible_practitioner_id` ; tables filles actes/examens/prescriptions internes ; contrainte lit occupé |
| V37 | `medical_documents.document_type`, `hash`, `qr_code_url`, `verification_url`, `author_user_id`, `version`, `previous_document_id`, `status` |
| V38 | `patient_consents` remodelée (type, statuts, durée, requester, canal) ; `external_access_requests.approved_at`, statut révoqué |

### Enums métier

- `DocumentType`, `DocumentStatus`
- `ConsentType`, `ConsentStatus`, `ValidationChannel`
- `LabOrderStatus`, `ExamType`
- `LabResultStatus`
- `PrescriptionStatus`

---

## 3. API REST

### Nouveaux endpoints

| Endpoint | Description | Rôle |
|---|---|---|
| `GET /api/patients/{id}/medical-summary` | Synthèse médicale structurée | Médecin, patient |
| `GET /api/patients/{id}/dpu` | Dossier patient agrégé (20 sections) | Médecin autorisé |
| `PATCH /api/prescriptions/{id}/cancel` | Annuler une ordonnance | Médecin |
| `PATCH /api/prescriptions/{id}/finalize` | DRAFT → ACTIVE | Médecin |
| `GET /api/patients/{id}/exam-results/export` | Export CSV/JSON résultats | Médecin, patient |
| `POST /api/lab-results/{id}/validate` | Valider un résultat | Biologiste |
| `GET /fhir/DiagnosticReport?patient={id}` | Export FHIR DiagnosticReport | Rôles autorisés |
| `POST /api/patient/access-requests/{id}/revoke` | Révoquer un accès externe approuvé | Patient |

### Endpoints modifiés

- `POST /api/visits` : nouveaux champs.
- `POST /api/consultations` : diagnostic découpé.
- `POST /api/lab-orders` : type enum, items fils.
- `POST /api/public/lab-integration/upload` : validator_user_id, conclusion.

---

## 4. Sécurité

- RBAC Spring Security sur tous les nouveaux endpoints.
- Contrôle IDOR via `PatientAccessGuardService` / `PatientService.validateAccess()`.
- Audit log sur toutes les actions sensibles.
- Scopes granulaires pour les consentements.
- Hash SHA-256 des documents.
- Secrets externalisés (`JWT_SECRET`, `LAB_API_KEY`).

---

## 5. Design system

- Tailwind CSS v4 CSS-first avec `@import "tailwindcss"` et `@theme`.
- Tokens CSS centralisés dans `styles.css`.
- Rayons : 4px à 6px recommandés, 8px maximum.
- Composants partagés : `<app-ui-button>`, `<app-ui-input>`, `<app-ui-card>`, `<app-page-header>`, `<app-status-badge>`.
- Thèmes light/dark via `ThemeService`.
- i18n via `assets/i18n/fr.json` / `en.json`.

---

## 6. Tests

### Backend

- Tests unitaires domaine/service.
- Tests d’intégration MockMvc pour les controllers.
- Tests de sécurité AuthN/AuthZ.
- Tests de repository/migration.

### Frontend

- Tests unitaires des composants/services/guards.
- `npm run lint`, `npm run test`, `npm run build`.

---

## 7. Déploiement

- Migrations Flyway additives uniquement.
- Configuration externalisée via variables d’environnement.
- CI/CD existante (`github/workflows/ci.yml`) à valider.

---

## 8. Risques techniques

| Risque | Mitigation |
|---|---|
| Remodelage DB important | Migrations additives, backup avant déploiement |
| Régression `PatientService` | Découpage progressif, tests exhaustifs |
| Performance synthèse | Requêtes optimisées, index, éventuellement cache |
| i18n externalisée | Chargement lazy, fallback FR |

---

## 9. Références

- `docs/standards/ARCHITECTURE-SOLID-RESPONSIBILITY-STANDARDS.md`
- `docs/standards/DESIGN-SYSTEM-STANDARDS.md`
- `docs/standards/UI-RADIUS-AND-SHADOW-STANDARDS.md`
- `docs/standards/DOCUMENTATION-FIRST.md`
- `docs/standards/CONFIGURATION-STANDARDS.md`
