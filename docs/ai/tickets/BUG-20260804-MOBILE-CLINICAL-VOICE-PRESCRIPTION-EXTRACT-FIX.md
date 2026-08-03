# BUG-20260804-MOBILE-CLINICAL-VOICE-PRESCRIPTION-EXTRACT-FIX — Support prescriptions/ordonnances, non-blocage de l'écoute et chunking sans découpage de segment

## 1. Objectif

Sécuriser l'extraction et l'affichage des ordonnances/examens dans l'assistant vocal mobile, éviter le blocage de la boucle d'écoute progressive lors de l'analyse IA, et corriger le découpage backend pour ne jamais scinder un segment audio en deux.

## 2. Critères d'acceptation

- [x] Le backend regroupe les segments en chunks sans scinder un segment individuel au milieu de son texte (`splitLongText` supprimé).
- [x] L'analyse progressive dans le coordinator Flutter s'exécute de manière non-bloquante (`unawaited`) sur la boucle de sérialisation audio pour ne pas gêner la captation continue.
- [x] Les champs `prescriptions` et `labOrders` sont supportés dans le domaine `ConsultationNote` et alimentés depuis la réponse d'extraction IA.
- [x] L'aperçu clinique mobile (`ClinicalAcceptedPreview`) affiche proprement les ordonnances et examens extraits.
- [x] La notification de fin de dictée avertit le praticien des éléments complémentaires (ordonnances/examens) à saisir séparément.

## 3. Pilotage projet

| Champ | Valeur |
|---|---|
| Epic parent | EPIC-0028 |
| User story parent | MOB-2816 / MOB-2821 |
| Sprint cible | Hors sprint / Hotfix |
| Priorité business | P0 |
| Complexité | S |
| Story points | 2 |
| Profil recommandé | Senior Flutter + Senior Spring |
| Effort estimé senior | 0.5j |
| Effort estimé intermédiaire | 1j |
| Effort estimé junior | Non recommandé |
| Responsable | Antigravity |
| Reviewer obligatoire | Tech Lead Flutter + Lead Backend |
| Risque fonctionnel | Faible |
| Risque technique | Faible |
| Dépendances | Aucun |
| Bloquants connus | Aucun |

## 4. Contexte analysé

- [x] `AGENTS.md` lu
- [x] `SKILL.md` lu
- [x] `PROJECT-TRACKING.md` lu
- [x] `CHANGELOG.md` lu
- [x] Code Flutter et Spring Boot concernés analysés

## 5. Plan d'action

- [x] Backend : mise à jour `AiClinicalCaptureRebuildService.java` pour flush sans découper les segments longs.
- [x] Backend : documentation de `ConsultationExtraction.java` pour `prescription` et `labOrders`.
- [x] Mobile : ajout des champs `prescriptions` et `labOrders` dans `ConsultationNote` et `clinical_voice_ai_api.dart`.
- [x] Mobile : intégration `unawaited` sur `analyzeProgressiveSegment` dans `clinical_voice_progressive_coordinator.dart`.
- [x] Mobile : mise à jour de `ClinicalAcceptedPreview` pour afficher les ordonnances/examens.
- [x] Mobile : mise à jour de `consultation_notes_sheet.dart` pour transmettre le brouillon initial et afficher la notification appropriée.

## 6. Fichiers modifiés

- `backend/src/main/java/com/joprelys/backend/ai/application/AiClinicalCaptureRebuildService.java`
- `backend/src/main/java/com/joprelys/backend/ai/domain/ConsultationExtraction.java`
- `mobile/lib/features/dashboard/application/clinical_voice_progressive_coordinator.dart`
- `mobile/lib/features/dashboard/data/clinical_voice_ai_api.dart`
- `mobile/lib/features/dashboard/domain/consultation_note.dart`
- `mobile/lib/features/dashboard/presentation/widgets/clinical_voice_assistant_sheet.dart`
- `mobile/lib/features/dashboard/presentation/widgets/clinical_voice_progressive_assistant_sections.dart`
- `mobile/lib/features/dashboard/presentation/widgets/clinical_voice_progressive_preview.dart`
- `mobile/lib/features/dashboard/presentation/widgets/clinical_voice_review_widgets.dart`
- `mobile/lib/features/dashboard/presentation/widgets/consultation_note_form_controllers.dart`
- `mobile/lib/features/dashboard/presentation/widgets/consultation_notes_sheet.dart`

## 7. Reste à faire

- [ ] Commiter les modifications avec un message de commit clair et structuré.
