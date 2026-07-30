# Technical Design — Saisie des Constantes Patient (Mobile Flutter)

## 1. Architecture & Emplacement des fichiers
Le composant suit l'architecture feature-first dans `mobile/lib/features/dashboard` :
- `domain/patient_vitals.dart` : Modèle immuable des constantes vitales client/serveur avec validation des plages médicales.
- `data/vitals_api.dart` : Gateway Riverpod branchée sur `ApiClient` pour consommer `GET /api/visits/{id}/vitals` et `POST /api/visits/{id}/vitals`.
- `application/vitals_controller.dart` : StateNotifier/Notifier Riverpod gérant l'état du formulaire, la validation et la soumission.
- `presentation/widgets/patient_vitals_sheet.dart` : Composant Modal Bottom Sheet réutilisable exposant les champs de saisie, les badges d'état et l'action d'enregistrement.
- `presentation/widgets/active_queue_section.dart` : Raccordement de l'événement `onTap` sur la carte patient pour ouvrir `PatientVitalsSheet`.

## 2. Dynamic IMC Calculation
`BMI = Weight (kg) / (Height (m) ^ 2)`
Le calcul s'exécute localement dès la saisie du poids et de la taille.

## 3. Stratégie de Test
- Unit test sur la gateway API `vitals_api_test.dart`.
- Test widget pour la saisie et la soumission du formulaire `patient_vitals_sheet_test.dart`.
