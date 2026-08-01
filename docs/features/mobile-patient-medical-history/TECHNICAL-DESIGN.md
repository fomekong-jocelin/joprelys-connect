# Design Technique — Historique Médical Mobile (MOB-2816)

## 1. Architecture des Composants (Flutter / Riverpod)
```text
mobile/lib/features/dashboard/
├── domain/
│   └── patient_history.dart        # Modèle de l'historique et des visites passées
├── data/
│   └── patient_history_api.dart    # Gateway & provider REST ApiClient
└── presentation/
    └── widgets/
        └── patient_history_sheet.dart  # Modal Bottom Sheet d'historique médical
```

## 2. Structure des données
- `MedicalBackground` (type: MEDICAL, SURGICAL, FAMILY, description, date)
- `PatientAllergy` (allergen, severity: LOW, MODERATE, SEVERE, reaction)
- `PastVisitSummary` (id, visitNumber, date, practitionerName, chiefComplaint, vitals, soapNote)
- `PatientMedicalHistory` (patientId, patientName, patientDpu, antecedents, allergies, pastVisits)

## 3. Conformité aux Standards
- Utilisation de `ApiClient` avec gestion centralisée des erreurs RFC 7807.
- Support 100% i18n via `AppLocalizations` et `DashboardLocalizations`.
- Utilisation de `Theme.of(context).colorScheme` et `AppDesignTokens`.
