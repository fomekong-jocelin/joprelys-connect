# Design Technique — Saisie & Consultation des Notes Cliniques Mobile (MOB-2814)

## 1. Architecture Applicative

```text
[ ActiveQueueSection ] ──(tap action)──> [ ConsultationNotesSheet ]
                                                  │
                                          [ ConsultationApi ]
                                                  │
                                           [ ApiClient ]
                                                  │
                                (GET/POST /api/visits/{id}/consultation-notes)
```

## 2. Modèle Domain Flutter

- Fichier : `mobile/lib/features/dashboard/domain/consultation_note.dart`
- Champs :
  - `subjective` (String) : Plaintes / Anamnèse
  - `objective` (String) : Examen physique
  - `assessment` (String) : Évaluation / Diagnostic
  - `plan` (String) : Plan de soins / Traitement
  - `updatedAt` (DateTime?) : Horodatage de dernière modification

## 3. Gateway & Network

- Interface : `ConsultationGateway`
- Implémentation : `ConsultationApi`
- Provider Riverpod : `consultationApiProvider`
- Endpoints :
  - `GET /api/visits/{id}/consultation-notes` -> `ConsultationNote?`
  - `POST /api/visits/{id}/consultation-notes` -> `ConsultationNote`

## 4. Design System & UX Constraints

- Arrondis : 8px (`AppDesignTokens.radiusLg`) max.
- Thèmes : Dark (`#071124`) & Light (`#F8FAFC`).
- i18n : Français (`fr`) et Anglais (`en`) via `DashboardLocalizations`.
