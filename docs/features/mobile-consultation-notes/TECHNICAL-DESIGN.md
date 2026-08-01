# Design Technique — Saisie & Consultation des Notes Cliniques Mobile (MOB-2821)

## 1. Architecture Applicative

```text
[ ActiveQueueSection ] ──(tap action)──> [ ConsultationNotesSheet ]
                                                  │
                                          [ ConsultationApi ]
                                                  │
                                           [ ApiClient ]
                                                  │
                                (GET/POST /api/visits/{id}/consultation)
```

## 2. Modèle Domain Flutter

- Fichier : `mobile/lib/features/dashboard/domain/consultation_note.dart`
- Le modèle simplifié `subjective/objective/assessment/plan` est remplacé par le
  contrat détaillé partagé :
  - `symptoms` : Subjectif ;
  - `clinicalExam` : Objectif ;
  - `diagnosis` : Évaluation ;
  - `conclusion`, `advice`, `followUp` : Plan ;
  - `updatedAt` parmi les métadonnées de consultation renvoyées par le backend.

## 3. Gateway & Network

- Interface : `ConsultationGateway`
- Implémentation : `ConsultationApi`
- Provider Riverpod : `consultationApiProvider`
- Endpoints canoniques :
  - `GET /api/visits/{id}/consultation` -> consultation détaillée ou `204` ;
  - `POST /api/visits/{id}/consultation` -> consultation détaillée.
- Aucun alias spécifique Flutter ne doit être créé.
- Le backend reste responsable de la validation métier. Les anciens champs
  `suspectedDiagnosis` et `finalDiagnosis` ne font plus partie du contrat actif.
- La migration `V109` archive leurs valeurs, choisit la valeur diagnostique la
  plus aboutie disponible, puis supprime les colonnes actives redondantes.

## 4. Présentation Flutter

- `ConsultationNotesSheet` orchestre le chargement et l'enregistrement.
- `ConsultationNotesHeader` porte l'en-tête patient et les actions.
- `ConsultationNotesForm` compose les quatre sections SOAP.
- `ConsultationNoteFormControllers` isole la gestion des six contrôleurs.
- `ClinicalTextArea` centralise les champs multi-lignes, limites et validations.
- Chaque fichier de cette surface reste sous 300 lignes.
- L'assistant vocal lié est séparé en `ClinicalVoiceAssistantSheet`,
  `ClinicalVoiceListeningSurface` et widgets de transcription ; aucun fichier ne
  dépasse 500 lignes, les deux premiers restant à rapprocher de la cible 300.

## 5. Présentation Angular

- `ClinicalNoteEditorComponent` rend explicitement les quatre sections S/O/A/P.
- `ConsultationComponent` orchestre le parcours et reste sous la limite dure de
  500 lignes après extraction.
- `ConsultationPrescriptionFacade` porte l'état et les actions de prescription.
- `ConsultationFeedbackStore` centralise les états de chargement et les messages.
- `ConsultationUiLabelsService` centralise les libellés et classes sémantiques
  des constantes.
- `ConsultationComponent` (481 lignes) et `ClinicalNoteEditorComponent` (358
  lignes) restent au-dessus de la cible d'alerte de 300 lignes et doivent être
  encore découpés avant extension fonctionnelle importante.

## 6. Design System & UX Constraints

- Arrondis : 8px (`AppDesignTokens.radiusLg`) max.
- Thèmes : Dark (`#071124`) & Light (`#F8FAFC`).
- i18n : Français (`fr`) et Anglais (`en`) via `DashboardLocalizations`.
